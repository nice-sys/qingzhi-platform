package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.dto.request.PasswordResetRequest;
import com.qingzhi.demo.dto.request.UserProfileUpdateRequest;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.entity.User;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.UserMapper;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.service.UserService;
import com.qingzhi.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户业务实现
 * <p>对应 PRD 2.2.2 管理员用户管理 + 2.2.3 普通用户个人信息管理</p>
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileService fileService;

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

    /* ====================================================================================
     * 3. 修改/补充个人信息（PRD 2.2.3「补充个人信息」）
     *    动态 SQL：仅非空字段会被更新，避免覆盖其他列
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        // 1. 基础参数校验
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.NOT_LOGGED_IN);
        BusinessException.throwIfNull(request,
                ResponseCodeEnum.PARAM_ERROR, "请求体不能为空");

        // 2. 用户存在性校验
        User existing = userMapper.selectById(userId);
        BusinessException.throwIf(existing == null,
                ResponseCodeEnum.FAILURE, "用户不存在");

        // 3. 如果 5 个字段全为空（或全空字符串）→ 直接返回旧信息，不写库，减少 IO
        boolean anyFieldToUpdate = (request.getName() != null && !request.getName().isEmpty())
                || (request.getPhone() != null && !request.getPhone().isEmpty())
                || (request.getEmail() != null && !request.getEmail().isEmpty())
                || (request.getDepartment() != null && !request.getDepartment().isEmpty())
                || (request.getMajor() != null && !request.getMajor().isEmpty());
        if (!anyFieldToUpdate) {
            return UserInfoResponse.fromEntity(existing);
        }

        // 4. 组装 patch 对象：只 set 非空字段（UserMapper.updateById 是动态 SQL，null 列不更新）
        User patch = new User();
        patch.setId(userId);
        if (request.getName() != null && !request.getName().isEmpty()) {
            patch.setName(request.getName());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            patch.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            patch.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
            patch.setDepartment(request.getDepartment());
        }
        if (request.getMajor() != null && !request.getMajor().isEmpty()) {
            patch.setMajor(request.getMajor());
        }

        // 5. 执行动态更新（@Transactional 保证失败自动回滚）
        int rows = userMapper.updateById(patch);
        BusinessException.throwIf(rows != 1,
                ResponseCodeEnum.FAILURE, "更新个人信息失败，请稍后重试");

        // 6. 回读最新数据并返回脱敏后的 UserInfoResponse（保证前端看到的与 DB 一致）
        User updated = userMapper.selectById(userId);
        return UserInfoResponse.fromEntity(updated);
    }

    /* ====================================================================================
     * 4. 更新当前登录用户的头像
     *    复用 FileService.uploadFile（支持秒传），头像文件保存到文件存储表
     *    avatar_url 存储形式：/api/file/download/{fileStorageId}（由前端转发拼接，或直接相对路径由 request 拼接）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateAvatar(Long userId, MultipartFile file) {
        // 1. 基础参数校验
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.NOT_LOGGED_IN);
        BusinessException.throwIfNull(file, ResponseCodeEnum.PARAM_ERROR, "请选择要上传的头像文件");
        BusinessException.throwIf(file.isEmpty(), ResponseCodeEnum.PARAM_ERROR, "头像文件不能为空");

        // 2. 校验用户存在
        User existing = userMapper.selectById(userId);
        BusinessException.throwIf(existing == null,
                ResponseCodeEnum.FAILURE, "用户不存在");

        // 3. 上传文件（复用 FileService，支持秒传、引用计数、大小/扩展名白名单）
        Map<String, Object> uploadResult = fileService.uploadFile(file, userId);
        Long fileStorageId = uploadResult != null && uploadResult.get("fileStorageId") != null
                ? ((Number) uploadResult.get("fileStorageId")).longValue()
                : null;
        BusinessException.throwIf(fileStorageId == null,
                ResponseCodeEnum.FAILURE, "头像存储失败，请稍后重试");

        // 4. 拼接 avatar_url（相对路径，前端可直接使用；由前端 request baseURL 自动补全 /api 前缀）
        String avatarUrl = "/api/file/download/" + fileStorageId;

        // 5. 如果旧头像也使用 file/download/{id} 形式，尝试释放旧引用
        String oldAvatarUrl = existing.getAvatarUrl();
        if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/api/file/download/")) {
            try {
                String oldIdStr = oldAvatarUrl.substring("/api/file/download/".length());
                Long oldFsId = Long.parseLong(oldIdStr);
                if (!oldFsId.equals(fileStorageId)) {
                    fileService.releaseReference(oldFsId);
                }
            } catch (Exception ignore) {
                // 旧引用释放失败不阻塞主流程
            }
        }

        // 6. 写库（动态 SQL，只更新 avatar_url + update_time）
        User patch = new User();
        patch.setId(userId);
        patch.setAvatarUrl(avatarUrl);
        int rows = userMapper.updateById(patch);
        BusinessException.throwIf(rows != 1,
                ResponseCodeEnum.FAILURE, "头像更新失败，请稍后重试");

        // 7. 回读并返回
        User updated = userMapper.selectById(userId);
        return UserInfoResponse.fromEntity(updated);
    }
}
