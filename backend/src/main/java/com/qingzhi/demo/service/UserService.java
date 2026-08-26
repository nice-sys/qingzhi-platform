package com.qingzhi.demo.service;

import com.qingzhi.demo.dto.request.PasswordResetRequest;
import com.qingzhi.demo.dto.request.UserProfileUpdateRequest;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户业务接口
 * <p>对应 PRD 2.2.2 管理员用户管理 + 2.2.3 普通用户个人信息管理</p>
 */
public interface UserService {

    /**
     * 获取当前登录用户的个人信息
     * <p>对应 PRD 2.2.3：查询/补充个人信息（教师/学生权限）</p>
     *
     * @param userId 当前登录用户ID（由 JwtInterceptor 从 Token 中提取）
     * @return 脱敏后的用户信息
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 修改当前登录用户的密码（自用）
     * <p>对应 PRD 2.1.3 密码管理：普通用户修改自己的密码
     * & PRD 2.2.1 权限矩阵：教师(Y)、学生(Y)、管理员(Y)</p>
     * <ol>
     *   <li>校验旧密码是否正确：错误 → 提示"旧密码不正确"</li>
     *   <li>禁止新密码与旧密码相同：相同 → 提示"新密码不能与旧密码相同"</li>
     *   <li>校验两次新密码一致：不一致 → 提示"两次输入的新密码不一致"</li>
     *   <li>新密码 MD5 加密后更新数据库</li>
     * </ol>
     *
     * @param userId  当前登录用户ID
     * @param request 修改密码请求（oldPassword + newPassword + confirmNewPassword）
     */
    void changePassword(Long userId, PasswordResetRequest request);

    /**
     * 修改/补充当前登录用户的个人信息（PRD 2.2.3）
     * <p>支持字段：name / phone / email / department / major（均可选；为 null 则不更新该列）。
     * <p>更新后返回最新的 UserInfoResponse（与 getUserInfo 格式一致，前端直接替换视图）。
     *
     * @param userId  当前登录用户ID
     * @param request 修改请求（仅非空字段参与更新，格式校验已在 Controller @Valid 完成）
     * @return 修改后的最新个人信息
     */
    UserInfoResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 更新当前登录用户的头像
     * <p>内部复用 FileService.uploadFile，将图片文件保存到文件存储（支持秒传），
     * 然后将返回的 fileStorageId 拼成 /api/file/download/{id} 作为 avatar_url。
     *
     * @param userId 当前登录用户ID
     * @param file   上传的图片文件（MultipartFile）
     * @return 修改后的最新个人信息（含 avatarUrl）
     */
    UserInfoResponse updateAvatar(Long userId, MultipartFile file);
}
