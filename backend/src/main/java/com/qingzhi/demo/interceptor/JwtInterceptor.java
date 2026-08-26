package com.qingzhi.demo.interceptor;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.config.JwtConfig;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 * <p>对应 PRD 3.2 安全需求：JWT 鉴权</p>
 * <p>流程：</p>
 * <ol>
 *   <li>从请求 Header 中提取 Authorization 字段</li>
 *   <li>移除 "Bearer " 前缀得到 Token</li>
 *   <li>调用 JwtUtil 校验 Token 有效性并解析出 uid/role/uname</li>
 *   <li>将用户信息存入 request 属性，供后续 Controller/Service 使用</li>
 * </ol>
 * <p>白名单路径（无需鉴权）由 WebConfig 配置，常见：/api/auth/**、/actuator/** 等</p>
 *
 * @see JwtUtil
 * @see com.qingzhi.demo.config.WebConfig
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. OPTIONS 预检请求直接放行（前后端分离跨域场景需要）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 匿名接口放行（白名单已被 WebConfig exclude 了；这里处理「白名单外但仍允许匿名」的公开 GET 接口，
        //    如 GET /resource/{id}（纯数字）、GET /resource/list。
        //    逻辑：
        //      · 若 Authorization 头有值 → 继续 JWT 校验（登录态不丢）
        //      · 若 Authorization 头为空 → 直接放行（userId=null 作为匿名用户）
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        // 匹配公开 GET 接口
        boolean isPublicGet = false;
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if ("/resource/list".equals(requestUri)
                    || "/resource/courses".equals(requestUri)
                    || "/resource/stats".equals(requestUri)) {
                isPublicGet = true;
            } else {
                // /resource/{id} ：正则 /resource/\d+
                if (requestUri.matches("^/resource/\\d+$")) {
                    isPublicGet = true;
                }
            }
        }

        // 3. 从 Header 获取 Token
        String header = request.getHeader(jwtConfig.getHeaderName());
        if (header == null || header.isEmpty()) {
            // 公开 GET 接口：匿名放行
            if (isPublicGet) {
                return true;
            }
            // 非公开接口：1001 未登录
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }

        // 4. 移除 Token 前缀（Bearer）
        String token;
        if (header.startsWith(jwtConfig.getTokenPrefix())) {
            token = header.substring(jwtConfig.getTokenPrefix().length());
        } else {
            token = header; // 兼容不带前缀的请求
        }
        if (token.isEmpty()) {
            if (isPublicGet) {
                return true;
            }
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }

        // 5. 解析 Token（此处会自动校验签名与过期时间）
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            // 公开 GET：JWT 过期/无效也允许匿名访问（至少能看到资源，只是未登录态）
            if (isPublicGet) {
                return true;
            }
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }

        // 6. 将解析出的用户信息存入 request 属性，供业务层使用
        Long userId = claims.get(jwtConfig.getClaimUserId(), Long.class);
        Integer role = claims.get(jwtConfig.getClaimRole(), Integer.class);
        String username = claims.get(jwtConfig.getClaimUsername(), String.class);

        request.setAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID, userId);
        request.setAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ROLE, role);
        request.setAttribute("currentUsername", username);

        return true;
    }

    /* ====================================================================================
     * 静态辅助方法：方便在 Controller/Service 中取当前登录用户
     * ==================================================================================== */

    /**
     * 从 HttpServletRequest 获取当前登录用户ID
     *
     * @param request 请求对象
     * @return 用户ID；未登录返回 null
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object val = request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
        return val instanceof Long ? (Long) val : null;
    }

    /**
     * 从 HttpServletRequest 获取当前登录用户角色
     *
     * @param request 请求对象
     * @return 角色编码；未登录返回 null
     */
    public static Integer getCurrentUserRole(HttpServletRequest request) {
        Object val = request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ROLE);
        return val instanceof Integer ? (Integer) val : null;
    }

    /**
     * 从 HttpServletRequest 获取当前登录用户名（账号）
     *
     * @param request 请求对象
     * @return 账号；未登录返回 null
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        Object val = request.getAttribute("currentUsername");
        return val instanceof String ? (String) val : null;
    }
}
