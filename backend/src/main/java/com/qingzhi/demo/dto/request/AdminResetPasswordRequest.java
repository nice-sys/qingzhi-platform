package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 管理员重置用户密码请求 DTO
 * <p>对应 PRD 2.2.1 权限矩阵：重置用户密码 仅管理员(Y)
 * & PRD 2.2.2 管理员用户管理：包括重置用户密码</p>
 */
public class AdminResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被重置密码的用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于0")
    private Long userId;

    /**
     * 新密码（明文，后端加密存储）
     * <p>管理员重置时仍然要求遵循新密码格式规则（>=8位且含数字+字母）</p>
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = Constants.PASSWORD_MIN_LENGTH, message = "密码长度不能少于 " + Constants.PASSWORD_MIN_LENGTH + " 位")
    @Pattern(regexp = Constants.PASSWORD_REGEX, message = "密码格式不合法：需不少于8位且包含数字和字母")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;

    public AdminResetPasswordRequest() {
    }

    /**
     * 两次新密码是否一致
     */
    public boolean isNewPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }

    /* ============================================================
     * Getter / Setter
     * ============================================================ */

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}
