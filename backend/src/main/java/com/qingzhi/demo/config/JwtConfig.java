package com.qingzhi.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>读取 application.yml 中 qingzhi.jwt 前缀的配置项</p>
 * <p>对应 PRD 3.2 安全需求：JWT 鉴权</p>
 */
@Configuration
@ConfigurationProperties(prefix = "qingzhi.jwt")
public class JwtConfig {

    /**
     * JWT 签名密钥（建议长度 >= 32 位）
     */
    private String secret = "QingZhi-Shared-Platform-JWT-Secret-2026-v1";

    /**
     * Token 过期时间（单位：秒）
     * <p>默认 24 小时 = 86400 秒（注册后自动登录等非 rememberMe 场景使用）</p>
     */
    private long expireSeconds = 86400L;

    /**
     * 「记住我」Token 过期时间（单位：秒）
     * <p>默认 7 天 = 604800 秒；勾选记住我时使用</p>
     */
    private long rememberExpireSeconds = 604800L;

    /**
     * 「普通登录不勾选记住我」Token 过期时间（单位：秒）
     * <p>默认 2 小时 = 7200 秒；符合用户测试约束</p>
     */
    private long normalExpireSeconds = 7200L;

    /**
     * Token 在请求 Header 中的字段名
     */
    private String headerName = "Authorization";

    /**
     * Token 前缀（Bearer Token）
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token Claim 中存储用户 ID 的 key
     */
    private String claimUserId = "uid";

    /**
     * Token Claim 中存储角色的 key
     */
    private String claimRole = "role";

    /**
     * Token Claim 中存储用户名的 key
     */
    private String claimUsername = "uname";

    /* ============================================================
     * Getter / Setter
     * ============================================================ */

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public long getRememberExpireSeconds() {
        return rememberExpireSeconds;
    }

    public void setRememberExpireSeconds(long rememberExpireSeconds) {
        this.rememberExpireSeconds = rememberExpireSeconds;
    }

    public long getNormalExpireSeconds() {
        return normalExpireSeconds;
    }

    public void setNormalExpireSeconds(long normalExpireSeconds) {
        this.normalExpireSeconds = normalExpireSeconds;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getClaimUserId() {
        return claimUserId;
    }

    public void setClaimUserId(String claimUserId) {
        this.claimUserId = claimUserId;
    }

    public String getClaimRole() {
        return claimRole;
    }

    public void setClaimRole(String claimRole) {
        this.claimRole = claimRole;
    }

    public String getClaimUsername() {
        return claimUsername;
    }

    public void setClaimUsername(String claimUsername) {
        this.claimUsername = claimUsername;
    }
}
