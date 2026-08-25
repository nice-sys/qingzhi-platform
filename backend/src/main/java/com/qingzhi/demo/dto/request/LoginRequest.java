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

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
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
}
