package com.qingzhi.demo.utils;

import com.qingzhi.demo.config.JwtConfig;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>对应 PRD 3.2 安全需求：JWT 鉴权</p>
 * <p>使用 JJWT 库实现 Token 生成、解析、有效性校验</p>
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;

    /* ====================================================================================
     * 一、生成 Token
     * ==================================================================================== */

    /**
     * 生成 JWT Token
     * <p>Payload 中包含：用户ID、角色、用户名</p>
     *
     * @param userId   用户ID
     * @param role     角色编码（0管理员/1教师/2学生）
     * @param username 账号
     * @return 完整 Token 字符串（不含前缀，前端拼接 Bearer 即可）
     */
    public String generateToken(Long userId, Integer role, String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + jwtConfig.getExpireSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(jwtConfig.getClaimUserId(), userId)
                .claim(jwtConfig.getClaimRole(), role)
                .claim(jwtConfig.getClaimUsername(), username)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ====================================================================================
     * 二、解析 Token
     * ==================================================================================== */

    /**
     * 解析 Token 并获取 Claims（已校验签名与过期时间）
     *
     * @param token 完整 Token（不含 Bearer 前缀）
     * @return Claims（包含 uid、role、uname 等信息）
     * @throws BusinessException Token 无效、过期、篡改时抛出 1001 未登录异常
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN, "登录已过期，请重新登录");
        } catch (UnsupportedJwtException | MalformedJwtException |
                 SignatureException | IllegalArgumentException e) {
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }
    }

    /* ====================================================================================
     * 三、便捷方法：从 Token 中提取具体字段
     * ==================================================================================== */

    /**
     * 从 Token 中提取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object uid = claims.get(jwtConfig.getClaimUserId());
        return uid != null ? ((Number) uid).longValue() : null;
    }

    /**
     * 从 Token 中提取角色编码
     */
    public Integer getRole(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get(jwtConfig.getClaimRole());
        return role != null ? ((Number) role).intValue() : null;
    }

    /**
     * 从 Token 中提取用户名（账号）
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        Object uname = claims.get(jwtConfig.getClaimUsername());
        return uname != null ? String.valueOf(uname) : null;
    }

    /**
     * 仅校验 Token 是否有效（不抛出异常，返回 true/false）
     *
     * @return true=有效；false=无效/过期/篡改
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效时间（秒）；过期或无效返回 0
     */
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0;
        } catch (BusinessException e) {
            return 0;
        }
    }

    /* ====================================================================================
     * 私有辅助方法
     * ==================================================================================== */

    /**
     * 根据配置的 secret 字符串生成 HMAC-SHA 密钥
     */
    private SecretKey getSecretKey() {
        byte[] bytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
