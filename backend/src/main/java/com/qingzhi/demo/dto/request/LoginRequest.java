package com.qingzhi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 用户登录请求 DTO
 * <p>对应 PRD 2.1.2 用户登录：账号 + 密码</p>
 */
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号（学号/工号/Admin）
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过 50 个字符")
    private String username;

    /**
     * 密码（明文，后端使用 MD5 加密后与数据库比对）
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 是否勾选「记住我」
     * <ul>
     *   <li>true  → Token 有效期 7 天，并允许下次在登录页自动回填用户名</li>
     *   <li>false → Token 有效期 2 小时，下次打开不回填用户名</li>
     * </ul>
     * <p>可空：默认 false</p>
     */
    private Boolean rememberMe;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
        this.rememberMe = false;
    }

    public LoginRequest(String username, String password, Boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 取 rememberMe 值（null 视为 false，避免 NPE）
     */
    public Boolean getRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }

    /**
     * 同上：用于 Spring MVC / MyBatis 属性访问时 isXxx 命名
     */
    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
