package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改个人信息请求 DTO（PRD 2.2.3 普通用户自用 - 查询/补充个人信息）
 * <p>所有字段都可选（动态更新），为 null 则不更新该列。
 * <p>非空字段会做长度/格式校验，非法直接抛 MethodArgumentNotValidException → 由全局异常捕获返回 PARAM_ERROR。
 */
public class UserProfileUpdateRequest {

    /**
     * 姓名（可选；非空则限制长度）
     */
    @Size(max = 50, message = "姓名长度不能超过 " + Constants.NAME_MAX_LENGTH + " 个字符")
    private String name;

    /**
     * 手机号（可选；非空则必须是合法中国大陆 11 位手机号）
     */
    @Size(max = 20, message = "手机号长度非法")
    @Pattern(regexp = Constants.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（可选；非空则必须是合法邮箱格式）
     */
    @Size(max = 100, message = "邮箱长度不能超过 " + Constants.EMAIL_MAX_LENGTH + " 个字符")
    @Pattern(regexp = Constants.EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    /**
     * 院系（可选；非空限制长度）
     */
    @Size(max = 100, message = "院系名称长度不能超过 " + Constants.DEPT_MAX_LENGTH + " 个字符")
    private String department;

    /**
     * 专业（仅学生；可选；非空限制长度。教师/管理员也可传，Service 层不限制角色，由业务自行决定展示）
     */
    @Size(max = 100, message = "专业名称长度不能超过 100 个字符")
    private String major;

    public String getName() { return name; }
    public void setName(String name) { this.name = (name == null) ? null : name.trim(); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = (phone == null) ? null : phone.trim(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = (email == null) ? null : email.trim(); }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = (department == null) ? null : department.trim(); }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = (major == null) ? null : major.trim(); }
}
