package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;

import java.io.Serializable;

/**
 * 用户管理查询请求 DTO（管理员用户列表分页查询）
 * <p>对应 PRD 2.2.2 管理员用户管理：对所有用户信息进行增删改查</p>
 */
public class UserQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（默认 1）
     */
    private Integer pageNum = Constants.DEFAULT_PAGE_NUM;

    /**
     * 每页条数（默认 10）
     */
    private Integer pageSize = Constants.DEFAULT_PAGE_SIZE;

    /**
     * 关键字搜索：账号(模糊) / 姓名(模糊)
     */
    private String keyword;

    /**
     * 按角色筛选：null=全部，0=管理员，1=教师，2=学生
     */
    private Integer role;

    /**
     * 按账号状态筛选：null=全部，0=正常，1=锁定
     */
    private Integer status;

    /**
     * 按院系筛选（模糊匹配）
     */
    private String department;

    public UserQueryRequest() {
    }

    /**
     * 规范化页码与每页条数，防止非法值
     */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = Constants.DEFAULT_PAGE_NUM;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > Constants.MAX_PAGE_SIZE) {
            pageSize = Constants.MAX_PAGE_SIZE;
        }
        // 关键字 trim
        if (keyword != null) {
            keyword = keyword.trim().isEmpty() ? null : keyword.trim();
        }
        if (department != null) {
            department = department.trim().isEmpty() ? null : department.trim();
        }
    }

    /**
     * 计算 offset（用于 SQL LIMIT offset, pageSize）
     */
    public int getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }

    public Integer getPageNum() {
        return pageNum == null ? Constants.DEFAULT_PAGE_NUM : pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null ? Constants.DEFAULT_PAGE_SIZE : pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
