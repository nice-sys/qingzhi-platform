package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.dto.request.PasswordResetRequest;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.entity.User;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.UserMapper;
import com.qingzhi.demo.service.UserService;
import com.qingzhi.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现
 * <p>对应 PRD 2.2.2 管理员用户管理 + 2.2.3 普通用户个人信息管理</p>
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /* ====================================================================================
     * 1. 获取当前登录用户的个人信息
     *    PRD 2.2.3：查询/补充个人信息（教师/学生权限）
     * ==================================================================================== */

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.NOT_LOGGED_IN);

        User user = userMapper.selectById(userId);
        BusinessException.throwIf(user == null,
                ResponseCodeEnum.FAILURE, "用户不存在");

        return UserInfoResponse.fromEntity(user);
    }

    /* ====================================================================================
     * 2. 修改当前登录用户的密码（自用）
     *    PRD 2.1.3 + 2.2.1 权限矩阵：教师(Y) 学生(Y) 管理员(Y)
     * ==================================================================================== */

    @Override
    public void changePassword(Long userId, PasswordResetRequest request) {

        // 1. 校验用户身份
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.NOT_LOGGED_IN);

        // 2. 校验两次新密码一致性（DTO层也做了，这里双保险，Service 单独判断）
        PasswordUtil.validateMatch(request.getNewPassword(), request.getConfirmNewPassword());

        // 3. 新密码格式校验（@Pattern 已在 DTO 层校验，Service 层再校验一次）
        PasswordUtil.validateFormat(request.getNewPassword());

        // 4. 禁止新密码与旧密码相同（安全加固）
        BusinessException.throwIf(request.isSameAsOld(),
                ResponseCodeEnum.FAILURE, "新密码不能与旧密码相同");

        // 5. 从数据库查询当前用户（确认用户存在并取出旧密码）
        User user = userMapper.selectById(userId);
        BusinessException.throwIf(user == null,
                ResponseCodeEnum.FAILURE, "用户不存在");

        // 6. 校验旧密码是否正确（明文 → MD5 后与数据库比对）
        boolean oldPasswordOk = PasswordUtil.checkPassword(request.getOldPassword(), user.getPassword());
        BusinessException.throwIf(!oldPasswordOk,
                ResponseCodeEnum.FAILURE, "旧密码不正确");

        // 7. 新密码 MD5 加密后更新数据库
        String encrypted = PasswordUtil.encrypt(request.getNewPassword());
        int rows = userMapper.updatePassword(userId, encrypted);
        BusinessException.throwIf(rows != 1,
                ResponseCodeEnum.FAILURE, "修改密码失败，请稍后重试");
    }
}
