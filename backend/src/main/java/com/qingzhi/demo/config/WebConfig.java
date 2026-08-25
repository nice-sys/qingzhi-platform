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
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")           // 默认拦截所有 /api/** 接口
                .excludePathPatterns(getWhitelist());  // 放行白名单
    }

    /**
     * 白名单路径（无需 JWT 鉴权）
     *
     * @return 白名单 URL 数组
     */
    private String[] getWhitelist() {
        return new String[]{
                // 1. 认证相关（注册+登录）
                "/api/auth/**",

                // 2. Actuator 监控端点
                "/actuator/**",

                // 3. 错误页面
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
