package com.qingzhi.demo.dto.response;

import java.io.Serializable;

/**
 * 登录响应 DTO
 * <p>注册成功也可复用此结构（注册后可以直接返回 Token 实现自动登录）</p>
 */
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JWT Token
     */
    private String token;

    /**
     * 当前登录用户信息（已脱敏，不含密码）
     */
    private UserInfoResponse userInfo;

    public LoginResponse() {
    }

    public LoginResponse(String token, UserInfoResponse userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }

    /**
     * 构造注册成功响应（不含 token）
     * <p>注册成功后可以直接返回用户信息，由前端跳转至登录页</p>
     */
    public static LoginResponse ofRegisterSuccess(UserInfoResponse userInfo) {
        return new LoginResponse(null, userInfo);
    }

    /**
     * 构造登录成功响应（含 token + 用户信息）
     */
    public static LoginResponse ofLoginSuccess(String token, UserInfoResponse userInfo) {
        return new LoginResponse(token, userInfo);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfoResponse getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoResponse userInfo) {
        this.userInfo = userInfo;
    }
}
