package com.qingzhi.demo.enums;

/**
 * 角色枚举
 * <p>对应 PRD 2.2.1 角色定义与权限矩阵</p>
 * <p>PRD 4.1 用户表 role 字段说明：0-管理员, 1-教师, 2-学生</p>
 */
public enum RoleEnum {

    /** 管理员（0）：用户管理、资源审核、Excel导入等 */
    ADMIN(0, "管理员"),

    /** 教师（1）：发布资源、收藏资源、管理个人信息等 */
    TEACHER(1, "教师"),

    /** 学生（2）：发布资源、收藏资源、管理个人信息等 */
    STUDENT(2, "学生");

    /** 角色编码（对应数据库 role 字段值） */
    private final int code;

    /** 角色中文描述 */
    private final String description;

    RoleEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据角色编码获取枚举
     *
     * @param code 角色编码（0/1/2）
     * @return 角色枚举，编码无效时返回 null
     */
    public static RoleEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否为管理员角色
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 判断是否为教师角色
     */
    public boolean isTeacher() {
        return this == TEACHER;
    }

    /**
     * 判断是否为学生角色
     */
    public boolean isStudent() {
        return this == STUDENT;
    }

    /**
     * 判断是否为普通用户（教师或学生，非管理员）
     */
    public boolean isNormalUser() {
        return this == TEACHER || this == STUDENT;
    }
}
