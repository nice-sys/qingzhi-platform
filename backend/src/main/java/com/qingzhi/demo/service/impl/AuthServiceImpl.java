package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.dto.request.LoginRequest;
import com.qingzhi.demo.dto.request.RegisterRequest;
import com.qingzhi.demo.dto.response.LoginResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.entity.User;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.UserMapper;
import com.qingzhi.demo.service.AuthService;
import com.qingzhi.demo.utils.JwtUtil;
import com.qingzhi.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证业务实现
 * <p>对应 PRD 2.1 用户认证模块</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /* ====================================================================================
     * 2.1.1 用户注册（自行注册）
     * PRD 规则：
     *  - 角色限制：管理员不可自行注册，只能通过 Excel 批量创建或系统内置
     *  - 学生必填专业；教师专业可为空
     *  - 密码：>=8位，含数字+字母（DTO层已用 @Pattern 预校验）
     *  - 双通道：Excel 已导入的账号也视为已存在，返回"账号已存在，请直接登录"（2001）
     *  - 加密：密码 MD5 加密存储
     * ==================================================================================== */

    @Override
    public UserInfoResponse register(RegisterRequest request) {

        // 1. 校验角色：只允许教师(1)/学生(2)自行注册，禁止管理员
        RoleEnum role = request.getRoleEnum();
        BusinessException.throwIf(role == null, ResponseCodeEnum.FAILURE, "角色参数不正确");
        BusinessException.throwIf(role.isAdmin(), ResponseCodeEnum.FAILURE, "管理员账号不可自行注册");
        BusinessException.throwIf(!role.isNormalUser(), ResponseCodeEnum.FAILURE, "不支持的注册角色");

        // 2.学生必须填写专业（PRD 4.1：专业学生必填教师可为空）
        if (role.isStudent() && (request.getMajor() == null || request.getMajor().trim().isEmpty())) {
            BusinessException.throwOf(ResponseCodeEnum.FAILURE, "学生必须填写专业");
        }

        // 3.校验两次密码一致性
        PasswordUtil.validateMatch(request.getPassword(), request.getConfirmPassword());

        // 4.密码格式校验（与 @Pattern 已做前端+后端双保险）
        PasswordUtil.validateFormat(request.getPassword());

        // 5.账号查重（PRD 双通道并行规则：Excel已导入也提示"账号已存在，请直接登录"）
        User existingUser = userMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            BusinessException.throwOf(ResponseCodeEnum.ACCOUNT_ALREADY_EXISTS, "账号已存在，请直接登录");
        }

        // 6.构建用户实体
        User user = buildUserFromRequest(request, role);

        // 7.插入数据库
        int rows = userMapper.insert(user);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "注册失败，请稍后重试");

        // 8.返回脱敏后的用户信息
        return UserInfoResponse.fromEntity(user);
    }

    /* ====================================================================================
     * 2.1.2 用户登录（账号+密码）
     * PRD 规则：
     *  - 账号不存在 → 2002 账号或密码错误（不单独提示账号不存在，防止枚举）
     *  - 账号已锁定（status=1 且未到期）→ 2003 账号已锁定
     *  - 密码错误 → 累计 login_fail_count；>=5次则锁定15分钟
     *  - 密码正确 → 清零失败次数，解锁账号
     *  - 登录成功 → 生成 JWT Token（含 uid、role、uname）
     * ==================================================================================== */

    @Override
    public LoginResponse login(LoginRequest request) {

        // 1. 根据账号查询用户（不存在→ 2002 错误）
        User user = userMapper.selectByUsername(request.getUsername());
        BusinessException.throwIf(user == null, ResponseCodeEnum.ACCOUNT_OR_PASSWORD_ERROR);

        // 2. 检查账号锁定状态（加分项：登录防暴力破解）
        checkAndReleaseLock(user);

        // 3. 校验密码（MD5 比对）
        boolean passwordOk = PasswordUtil.checkPassword(request.getPassword(), user.getPassword());
        if (!passwordOk) {
            handleLoginFail(user);  // 密码错误：累计失败次数 + 可能锁定
            BusinessException.throwOf(ResponseCodeEnum.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 4. 密码正确 → 重置登录失败信息，解锁账号
        userMapper.resetLoginFailInfo(user.getId());

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getUsername());

        // 6. 返回 Token + 脱敏用户信息
        UserInfoResponse userInfo = UserInfoResponse.fromEntity(user);
        return LoginResponse.ofLoginSuccess(token, userInfo);
    }

    /* ====================================================================================
     * 私有辅助方法
     * ==================================================================================== */

    /**
     * 根据注册请求构造 User 实体
     * <p>自动处理：密码加密、状态默认值、时间戳赋值、角色转换等</p>
     */
    private User buildUserFromRequest(RegisterRequest request, RoleEnum role) {
        User user = new User();

        // 基础信息
        user.setUsername(request.getUsername().trim());
        user.setName(request.getName().trim());
        user.setPassword(PasswordUtil.encrypt(request.getPassword())); // MD5加密

        // 角色：只允许教师和学生
        user.setRoleEnum(role);

        // 联系信息（允许为空，去空格）
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));

        // 院系专业：院系必填；专业只对学生生效
        user.setDepartment(request.getDepartment().trim());
        if (role.isStudent()) {
            user.setMajor(request.getMajor().trim());
        } else {
            user.setMajor(trimToNull(request.getMajor()));
        }

        // 账号默认状态：0-正常，失败次数=0
        user.setStatus(0);
        user.setLoginFailCount(0);
        user.setLockTime(null);

        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        return user;
    }

    /**
     * trim 字符串，空白字符串转换为 null
     */
    private String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /* ---------------- 登录防暴力破解相关辅助方法 ---------------- */

    /**
     * 检查并自动解除过期的锁定状态
     * <p>规则：锁定时间超过 ACCOUNT_LOCK_DURATION_MINUTES（15分钟）后自动解锁</p>
     */
    private void checkAndReleaseLock(User user) {
        // 若状态未锁定，直接通过
        if (user.isNormal()) {
            return;
        }
        // 已锁定 → 检查是否到期（到期则自动解锁，允许此次登录继续）
        if (user.getLockTime() != null) {
            LocalDateTime expireAt = user.getLockTime()
                    .plusMinutes(Constants.ACCOUNT_LOCK_DURATION_MINUTES);
            if (LocalDateTime.now().isAfter(expireAt)) {
                // 锁定已到期 → 自动解锁并清零失败次数（但密码校验仍要继续）
                userMapper.resetLoginFailInfo(user.getId());
                return;
            }
        }
        // 锁定未到期 → 抛出 2003
        BusinessException.throwOf(ResponseCodeEnum.ACCOUNT_LOCKED);
    }

    /**
     * 处理登录失败：累计失败次数 + 达到阈值锁定账号
     * <p>PRD 加分项：同一账号 5 分钟内连续失败 5 次，锁定 15 分钟</p>
     */
    private void handleLoginFail(User user) {
        int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        User update = new User();
        update.setId(user.getId());
        update.setLoginFailCount(failCount);

        if (failCount >= Constants.LOGIN_FAIL_THRESHOLD) {
            // 达到阈值：锁定账号
            update.setStatus(1);
            update.setLockTime(LocalDateTime.now());
        } else {
            // 未达到阈值：保持原状态，不锁定
            update.setStatus(user.getStatus());
            update.setLockTime(user.getLockTime());
        }

        userMapper.updateLoginFailInfo(update);
    }
}
