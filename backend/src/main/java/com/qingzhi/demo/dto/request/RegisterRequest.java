package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.enums.RoleEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 用户注册请求 DTO
 * <p>对应 PRD 2.1.1 用户注册（自行注册方式），教师和学生都可以自行注册</p>
 */
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号（学号或工号）
     * <p>PRD：学生 → 学号作为账号；教师 → 工号作为账号</p>
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = Constants.USERNAME_MAX_LENGTH, message = "账号长度不能超过 " + Constants.USERNAME_MAX_LENGTH + " 个字符")
    private String username;

    /**
     * 密码（明文，后端会加密存储）
     * <p>PRD 2.1.1：>=8位，须含数字+字母；前后端均需校验</p>
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = Constants.PASSWORD_REGEX, message = "密码格式不合法：需不少于8位且包含数字和字母")
    private String password;

    /**
     * 确认密码（前端也会做一致性校验，后端也需校验）
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 角色：1-教师, 2-学生
     * <p>PRD：管理员不可自行注册，只能是系统内置 Admin 账号</p>
     */
    @NotNull(message = "角色不能为空")
    private Integer role;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = Constants.NAME_MAX_LENGTH, message = "姓名长度不能超过 " + Constants.NAME_MAX_LENGTH + " 个字符")
    private String name;

    /**
     * 手机号（选填）
     */
    @Size(max = Constants.PHONE_LENGTH, message = "手机号长度不正确")
    @Pattern(regexp = "^$|" + Constants.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（选填）
     */
    @Size(max = Constants.EMAIL_MAX_LENGTH, message = "邮箱长度不能超过 " + Constants.EMAIL_MAX_LENGTH + " 个字符")
    @Pattern(regexp = "^$|" + Constants.EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    /**
     * 院系（必填）
     * <p>PRD 2.4.1 Excel 表头示例：院系 是必填项</p>
     */
    @NotBlank(message = "院系不能为空")
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "院系长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 个字符")
    private String department;

    /**
     * 专业（仅学生必填，教师可为空）
     * <p>PRD 4.1：专业 学生必填，教师可为空</p>
     */
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "专业长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 个字符")
    private String major;

    public RegisterRequest() {
    }

    /* ============================================================
     * 便捷方法
     * ============================================================ */

    /**
     * 获取角色枚举
     */
    public RoleEnum getRoleEnum() {
        return RoleEnum.of(this.role);
    }

    /**
     * 两次密码是否一致
     */
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /* ============================================================
     * Getter / Setter
     * ============================================================ */

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }
}
