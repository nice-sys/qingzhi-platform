package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.dto.request.LoginRequest;
import com.qingzhi.demo.dto.request.RegisterRequest;
import com.qingzhi.demo.dto.response.LoginResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器（注册 + 登录）
 * <p>对应 PRD 2.1 用户认证模块；所有接口不携带 JWT 即可访问（白名单）</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /* ====================================================================================
     * 2.1.1 用户注册（自行注册）
     *  - 请求方式：POST（PRD 5.2 创建/提交操作使用 POST）
     *  - 路径：POST /api/auth/register
     *  - 请求体：RegisterRequest（@Valid 触发 Bean Validation 校验）
     *  - 成功响应：code=1, message=注册成功, data=LoginResponse（注册成功返回用户信息，不含 Token）
     *  - 失败响应（常见）：
     *      code=2001 账号已存在，请直接登录
     *      code=2004 密码格式不合法
     *      code=0    其他业务错误（管理员不可注册、专业必填等）
     * ==================================================================================== */

    /**
     * 用户注册
     * <p>教师和学生可通过此接口自行注册；管理员不可自行注册</p>
     *
     * @param request 注册请求（密码、确认密码、角色、学号/工号、姓名、院系、专业等）
     * @return 注册成功后的用户信息（已脱敏，不含密码）
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success("注册成功", response);
    }

    /* ====================================================================================
     * 2.1.2 用户登录（账号+密码）
     *  - 请求方式：POST
     *  - 路径：POST /api/auth/login
     *  - 请求体：LoginRequest（username + password）
     *  - 成功响应：code=1, message=登录成功, data=LoginResponse（含 token + 用户信息）
     *  - 失败响应（常见）：
     *      code=2002 账号或密码错误（不区分账号不存在或密码错误，防止枚举）
     *      code=2003 账号已锁定，请稍后再试（5分钟内连续失败5次，锁定15分钟）
     *      code=1001 未登录（仅在需要鉴权的接口返回，登录本身不会返回）
     * ==================================================================================== */

    /**
     * 用户登录
     * <p>支持管理员（Admin）、教师（工号）、学生（学号）登录</p>
     *
     * @param request 登录请求（账号 + 密码）
     * @return 登录响应（JWT Token + 脱敏用户信息）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }
}
