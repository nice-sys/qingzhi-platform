package com.qingzhi.demo.entity;

import com.qingzhi.demo.enums.RoleEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>对应 PRD 4.1 用户表（user）</p>
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（BIGINT, PK, AUTO_INCREMENT）
     */
    private Long id;

    /**
     * 账号（学号/工号/Admin）（VARCHAR, UNIQUE, NOT NULL）
     */
    private String username;

    /**
     * 加密后的密码（VARCHAR, NOT NULL）
     */
    private String password;

    /**
     * 姓名（VARCHAR）
     */
    private String name;

    /**
     * 手机号（VARCHAR）
     */
    private String phone;

    /**
     * 邮箱（VARCHAR）
     */
    private String email;

    /**
     * 院系（VARCHAR）
     */
    private String department;

    /**
     * 专业（仅学生）（VARCHAR）
     */
    private String major;

    /**
     * 头像URL（VARCHAR）
     */
    private String avatarUrl;

    /**
     * 角色编码（TINYINT）：0-管理员, 1-教师, 2-学生
     * <p>对应 {@link RoleEnum}</p>
     */
    private Integer role;

    /**
     * 账号状态（TINYINT, NOT NULL, DEFAULT 0）：0-正常, 1-锁定
     */
    private Integer status;

    /**
     * 连续登录失败次数（INT, DEFAULT 0）
     * <p>对应 PRD 加分项：登录防暴力破解</p>
     */
    private Integer loginFailCount;

    /**
     * 锁定开始时间（DATETIME, 可为空）
     * <p>配合 loginFailCount 使用，超过 LOGIN_FAIL_THRESHOLD 次后写入</p>
     */
    private LocalDateTime lockTime;

    /**
     * 创建时间（DATETIME, NOT NULL）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（DATETIME, NOT NULL）
     */
    private LocalDateTime updateTime;

    public User() {
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
     * 设置角色枚举
     */
    public void setRoleEnum(RoleEnum roleEnum) {
        this.role = roleEnum != null ? roleEnum.getCode() : null;
    }

    /**
     * 判断账号是否为正常状态（未锁定）
     */
    public boolean isNormal() {
        return this.status != null && this.status == 0;
    }

    /**
     * 判断账号是否已锁定
     */
    public boolean isLocked() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        RoleEnum re = getRoleEnum();
        return re != null && re.isAdmin();
    }

    /**
     * 判断是否为教师
     */
    public boolean isTeacher() {
        RoleEnum re = getRoleEnum();
        return re != null && re.isTeacher();
    }

    /**
     * 判断是否为学生
     */
    public boolean isStudent() {
        RoleEnum re = getRoleEnum();
        return re != null && re.isStudent();
    }

    /* ============================================================
     * Getter / Setter
     * ============================================================ */

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
