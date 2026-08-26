package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.dto.request.PasswordResetRequest;
import com.qingzhi.demo.dto.request.UserProfileUpdateRequest;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器（普通用户个人中心 / 自用）
 * <p>对应 PRD 2.2.3 普通用户权限：查询/补充个人信息、修改密码、我的资源、我的收藏等
 * <p>此控制器下所有接口均需 JWT 鉴权（/api/user/** 不在白名单中）</p>
 */
@RestController
@RequestMapping("/user")
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
     * 2. 修改/补充当前登录用户的个人信息
     *    PRD 2.2.3「补充个人信息」：name / phone / email / department / major
     *    方式：POST /api/user/info（与查询同路径，不同 HTTP method，RESTful 风格）
     * ==================================================================================== */

    /**
     * 修改/补充当前登录用户的个人信息
     * <p>所有字段均可选：传 null 则不更新该列（动态 SQL 更新），传空字符串 "" 等价于不改。
     * <p>非空字段会通过 {@link Valid} 做长度/格式校验（手机号 11 位、邮箱格式、各列长度上限等），
     * 非法直接返回 PARAM_ERROR。
     *
     * @param body    要修改的字段（仅非空字段参与更新）
     * @param request HTTP 请求（用于获取 JWT 中的用户ID）
     * @return 修改后的最新个人信息（与 GET /api/user/info 结构一致，前端直接替换即可）
     */
    @PostMapping("/info")
    public Result<UserInfoResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest body,
            HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        UserInfoResponse updated = userService.updateProfile(userId, body);
        return Result.success(updated);
    }

    /* ====================================================================================
     * 3. 修改当前登录用户的密码（自用）
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

    /* ====================================================================================
     * 4. 更新当前登录用户的头像
     *    PRD 补充：个人中心头像上传
     *    方式：POST /api/user/avatar   Content-Type: multipart/form-data
     *    表单字段：file（必填，头像图片）
     * ==================================================================================== */

    /**
     * 更新当前登录用户的头像
     * <ol>
     *   <li>复用 FileService.uploadFile（支持秒传 + 引用计数）</li>
     *   <li>将 avatar_url 保存为 /api/file/download/{id} 相对路径</li>
     *   <li>旧头像（若也为 file/download 形式）会尝试 releaseReference 释放引用</li>
     * </ol>
     *
     * @param file    头像图片（MultipartFile，表单字段名：file）
     * @param request HTTP 请求（用于提取 JWT userId）
     * @return 更新后的 UserInfoResponse（含 avatarUrl）
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserInfoResponse> updateAvatar(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        UserInfoResponse updated = userService.updateAvatar(userId, file);
        return Result.success(updated);
    }
}
