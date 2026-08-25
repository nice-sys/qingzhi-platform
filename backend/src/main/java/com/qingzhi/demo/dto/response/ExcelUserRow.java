package com.qingzhi.demo.dto.response;

import java.io.Serializable;

/**
 * Excel 行数据 DTO（学生/教师导入共用）
 *
 * <p>学生 Excel 列索引（PRD 2.4.1）：
 * 0-学号, 1-姓名, 2-手机号, 3-邮箱, 4-院系, 5-专业, 6-初始密码</p>
 *
 * <p>教师 Excel 列索引（PRD 2.4.1）：
 * 0-工号, 1-姓名, 2-手机号, 3-邮箱, 4-院系, 5-初始密码</p>
 */
public class ExcelUserRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Excel 行号（从 2 开始，首行为表头） */
    private int rowNum;

    /** 学号/工号（登录账号 username） */
    private String username;

    /** 姓名 */
    private String name;

    /** 手机号（选填） */
    private String phone;

    /** 邮箱（选填） */
    private String email;

    /** 院系（必填） */
    private String department;

    /** 专业（仅学生必填；教师可为空） */
    private String major;

    /** 初始密码（明文，Service 加密入库） */
    private String password;

    public ExcelUserRow() {
    }

    /**
     * 学生 Excel 是否必填专业：是
     */
    public boolean isStudentRow() {
        // 填充 major 即认为是学生；Service 层按 role 参数也会再次判断
        return true;
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
