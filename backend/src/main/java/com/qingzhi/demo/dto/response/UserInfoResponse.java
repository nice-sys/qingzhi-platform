package com.qingzhi.demo.dto.response;

import com.qingzhi.demo.enums.RoleEnum;

import java.io.Serializable;

/**
 * 用户信息响应 DTO
 * <p>用于注册成功、登录成功、查询个人信息等场景，对 User 实体做脱敏（不返回 password 等敏感字段）</p>
 */
public class UserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 账号（学号/工号/Admin）
     */
    private String username;

    /**
     * 姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 院系
     */
    private String department;

    /**
     * 专业
     */
    private String major;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色编码：0-管理员, 1-教师, 2-学生
     */
    private Integer role;

    /**
     * 角色名称（方便前端直接展示）
     */
    private String roleName;

    /**
     * 账号状态：0-正常, 1-锁定
     */
    private Integer status;

    public UserInfoResponse() {
    }

    /**
     * 通过 User 实体构造（自动脱敏 + 填充 roleName）
     *
     * @param user 用户实体
     * @return 用户信息响应
     */
    public static UserInfoResponse fromEntity(com.qingzhi.demo.entity.User user) {
        if (user == null) {
            return null;
        }
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setName(user.getName());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setDepartment(user.getDepartment());
        resp.setMajor(user.getMajor());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setRole(user.getRole());
        resp.setStatus(user.getStatus());

        RoleEnum roleEnum = user.getRoleEnum();
        if (roleEnum != null) {
            resp.setRoleName(roleEnum.getDescription());
        }
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
