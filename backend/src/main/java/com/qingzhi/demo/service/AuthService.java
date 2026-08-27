package com.qingzhi.demo.service;

import com.qingzhi.demo.dto.request.LoginRequest;
import com.qingzhi.demo.dto.request.RegisterRequest;
import com.qingzhi.demo.dto.response.LoginResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;

/**
 * 认证业务接口
 * <p>对应 PRD 2.1 用户认证模块</p>
 */
public interface AuthService {

    /**
     * 用户注册（自行注册方式）
     * <p>对应 PRD 2.1.1 用户注册：双通道并行规则</p>
     * <ol>
     *   <li>校验角色：管理员不可自行注册</li>
     *   <li>校验学生专业必填：学生必须填写专业</li>
     *   <li>校验两次密码一致性</li>
     *   <li>账号查重：学号/工号已存在（含 Excel 已导入）则抛出 2001 "账号已存在，请直接登录"</li>
     *   <li>密码加密：使用 MD5 加密存储</li>
     *   <li>插入用户记录，默认账号状态 0-正常</li>
     *   <li>生成 JWT Token，注册后自动登录</li>
     * </ol>
     *
     * @param request 注册请求参数
     * @return 注册成功响应（JWT Token + 脱敏用户信息，与登录成功同结构）
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 用户登录（账号+密码）
     * <p>对应 PRD 2.1.2 用户登录</p>
     * <ol>
     *   <li>校验账号是否存在：不存在返回 2002 "账号或密码错误"</li>
     *   <li>检查账号锁定状态：已锁定（5分钟内失败 >=5次）返回 2003</li>
     *   <li>校验密码：错误则累计登录失败次数，达到阈值锁定15分钟</li>
     *   <li>登录成功：清零失败次数 + 解锁账号</li>
     *   <li>生成 JWT Token，返回 Token + 用户信息</li>
     * </ol>
     *
     * @param request 登录请求（账号 + 密码）
     * @return 登录响应（JWT Token + 脱敏用户信息）
     */
    LoginResponse login(LoginRequest request);
}
