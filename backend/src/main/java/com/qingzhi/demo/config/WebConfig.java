package com.qingzhi.demo.config;

import com.qingzhi.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * <p>注册 JWT 鉴权拦截器 + 配置跨域</p>
 * <p>对应 PRD 3.2 安全需求</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /* ====================================================================================
     * 一、注册 JWT 鉴权拦截器
     * ==================================================================================== */

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /* ====================================================================================
         * 注意：server.servlet.context-path=/api，所以这里的路径相对于 context-path，
         * 即真实请求 URL 为 /api/auth/login → 映射到的 Controller 路径是 /auth/login。
         * 因此拦截器要匹配 /**（context-path 后面的部分），
         * 白名单是 /auth/**、/file/download/**（匿名下载也放行）。
         * ==================================================================================== */
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")              // 拦截所有
                .excludePathPatterns(getWhitelist()); // 放行白名单
    }

    /**
     * 白名单路径（相对于 context-path 之后的部分，因为 context-path=/api 已被 Servlet 剥掉）
     *
     * @return 白名单 URL 数组
     */
    private String[] getWhitelist() {
        return new String[]{
                // 1. 认证相关（注册+登录）
                "/auth/**",

                // 2. 文件下载（允许匿名下载过审资源，鉴权在 Controller 层按 reviewStatus 判断）
                "/file/download/**",

                // 3. 资源模块 - 仅公开的「GET 查询」接口（匿名用户可浏览，不涉及修改）
                //    - 资源列表：GET /resource/list?page=1&size=10&keyword=&course=
                //    - 资源详情：GET /resource/{id}  →  这里因为 AntPathMatcher /** 最宽松匹配，无法按 method 区分，
                //                                   所以我们改用**仅放行 resource/list / resource/courses / resource/stats 等具名路径**，
                //                                   详情 {id} 改为走 JwtInterceptor（JWT 无效时也允许匿名通过：见 JwtInterceptor 注释）
                "/resource/list",
                "/resource/courses",
                "/resource/stats",

                // 4. Actuator 监控端点
                "/actuator/**",

                // 5. 错误页面
                "/error"
        };
    }

    /* ====================================================================================
     * 二、跨域配置（前后端分离 + Vue 开发模式需要）
     * ==================================================================================== */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")             // 允许所有来源（生产环境建议指定域名）
                .allowedMethods("GET", "POST", "PUT",
                        "DELETE", "OPTIONS", "PATCH")  // 允许的 HTTP 方法
                .allowedHeaders("*")                    // 允许所有 Header
                .exposedHeaders("Authorization",        // 允许前端读取的响应 Header
                        "Content-Disposition")
                .allowCredentials(true)                 // 允许携带 Cookie
                .maxAge(3600);                          // 预检请求缓存 1 小时
    }
}
