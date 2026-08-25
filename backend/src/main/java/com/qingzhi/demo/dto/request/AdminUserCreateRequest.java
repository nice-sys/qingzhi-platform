package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 管理员新增用户请求 DTO
 * <p>PRD 2.2.2 用户管理：对所有用户信息进行增删改查</p>
 */
public class AdminUserCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号（学号/工号/自定义）
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = Constants.USERNAME_MAX_LENGTH, message = "账号长度不能超过 " + Constants.USERNAME_MAX_LENGTH + " 字符")
    private String username;

    /**
     * 初始密码（明文）
     */
    @NotBlank(message = "初始密码不能为空")
    @Size(min = Constants.PASSWORD_MIN_LENGTH, message = "密码长度不能少于 " + Constants.PASSWORD_MIN_LENGTH + " 位")
    @Pattern(regexp = Constants.PASSWORD_REGEX, message = "密码格式不合法：需不少于8位且包含数字和字母")
    private String password;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = Constants.NAME_MAX_LENGTH, message = "姓名长度不能超过 " + Constants.NAME_MAX_LENGTH + " 字符")
    private String name;

    /**
     * 角色编码：0-管理员，1-教师，2-学生
     */
    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色编码非法")
    @Max(value = 2, message = "角色编码非法")
    private Integer role;

    /**
     * 手机号（选填）
     */
    @Pattern(regexp = Constants.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（选填）
     */
    @Pattern(regexp = Constants.EMAIL_REGEX, message = "邮箱格式不正确")
    @Size(max = Constants.EMAIL_MAX_LENGTH, message = "邮箱长度不能超过 " + Constants.EMAIL_MAX_LENGTH + " 字符")
    private String email;

    /**
     * 院系（选填，但 2.4 Excel 导入学生/教师是必填）
     */
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "院系长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 字符")
    private String department;

    /**
     * 专业（仅学生；选填）
     */
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "专业长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 字符")
    private String major;

    /**
     * 账号状态（默认 0-正常，不传由后端兜底）
     */
    @Min(value = 0, message = "状态编码非法")
    @Max(value = 1, message = "状态编码非法")
    private Integer status;

    public AdminUserCreateRequest() {
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
