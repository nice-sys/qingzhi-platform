package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.dto.request.PasswordResetRequest;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器（普通用户个人中心 / 自用）
 * <p>对应 PRD 2.2.3 普通用户权限：查询/补充个人信息、修改密码、我的资源、我的收藏等
 * <p>此控制器下所有接口均需 JWT 鉴权（/api/user/** 不在白名单中）</p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /* ====================================================================================
     * 1. 获取当前登录用户的个人信息
     *    PRD 2.2.3：查询/补充个人信息（教师/学生权限）
     *    方式：GET /api/user/info
     * ==================================================================================== */

    /**
     * 获取当前登录用户的个人信息
     * <p>用户ID从 JWT 中自动提取，不需要前端传参</p>
     *
     * @param request HTTP 请求（用于获取 JwtInterceptor 注入的用户ID）
     * @return 脱敏后的用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        UserInfoResponse userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /* ====================================================================================
     * 2. 修改当前登录用户的密码（自用）
     *    PRD 2.1.3 密码管理 + 2.2.1 权限矩阵：教师(Y) 学生(Y) 管理员(Y)
     *    方式：POST /api/user/password
     * ==================================================================================== */

    /**
     * 修改当前登录用户的密码
     * <ol>
     *   <li>用户ID从 JWT 中自动提取，不可通过参数修改他人密码</li>
     *   <li>需提供旧密码 + 新密码 + 确认新密码</li>
     *   <li>新密码必须满足格式要求（>=8位且含数字+字母）</li>
     *   <li>新密码不能与旧密码相同</li>
     * </ol>
     *
     * @param requestBody 修改密码请求（3 个密码字段）
     * @param request     HTTP 请求（用于获取 JWT 中的用户ID）
     * @return 成功：code=1，message="修改密码成功"；失败：对应错误码
     */
    @PostMapping("/password")
    public Result<Void> changePassword(
            @Valid @RequestBody PasswordResetRequest requestBody,
            HttpServletRequest request) {

        Long userId = JwtInterceptor.getCurrentUserId(request);
        userService.changePassword(userId, requestBody);
        return Result.success("修改密码成功", null);
    }
}
