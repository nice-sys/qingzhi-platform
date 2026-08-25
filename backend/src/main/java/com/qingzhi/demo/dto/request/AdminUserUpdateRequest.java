package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 管理员编辑用户信息请求 DTO
 * <p>所有字段可选（null 表示不修改，非空才更新），
 * 避免前端需要把不变字段也原封不动传回来。</p>
 */
public class AdminUserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 姓名（可空：不修改）
     */
    @Size(max = Constants.NAME_MAX_LENGTH, message = "姓名长度不能超过 " + Constants.NAME_MAX_LENGTH + " 字符")
    private String name;

    /**
     * 角色（可空：不修改）
     */
    @Min(value = 0, message = "角色编码非法")
    @Max(value = 2, message = "角色编码非法")
    private Integer role;

    /**
     * 手机号（可空：不修改）
     */
    @Pattern(regexp = Constants.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（可空：不修改）
     */
    @Pattern(regexp = Constants.EMAIL_REGEX, message = "邮箱格式不正确")
    @Size(max = Constants.EMAIL_MAX_LENGTH, message = "邮箱长度不能超过 " + Constants.EMAIL_MAX_LENGTH + " 字符")
    private String email;

    /**
     * 院系（可空：不修改）
     */
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "院系长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 字符")
    private String department;

    /**
     * 专业（可空：不修改）
     */
    @Size(max = Constants.DEPT_MAX_LENGTH, message = "专业长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 字符")
    private String major;

    /**
     * 账号状态（可空：不修改）
     */
    @Min(value = 0, message = "状态编码非法")
    @Max(value = 1, message = "状态编码非法")
    private Integer status;

    public AdminUserUpdateRequest() {
    }

    /**
     * 是否所有字段都为 null（全部不更新直接短路）
     */
    public boolean isAllNull() {
        return name == null && role == null && phone == null && email == null
                && department == null && major == null && status == null;
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

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
