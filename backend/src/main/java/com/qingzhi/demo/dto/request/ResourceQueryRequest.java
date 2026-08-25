package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 资源查询请求 DTO（管理员 / 普通用户 共用基础查询条件）
 * <p>对应 PRD 3.1 分页 + 时间范围查询（按发布时间指定起止日期，结果按时间倒序）</p>
 */
public class ResourceQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = Constants.DEFAULT_PAGE_NUM;
    private Integer pageSize = Constants.DEFAULT_PAGE_SIZE;

    /** 关键字搜索：标题 / 描述 / 课程 模糊 */
    private String keyword;

    /** 所属课程（精确或模糊，此处按模糊匹配） */
    private String course;

    /** 审核状态：null 全部 / 0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer reviewStatus;

    /** 上传者用户ID（用于"我的资源"筛选；管理员可用于指定发布者筛选） */
    private Long uploaderId;

    /** 发布时间起（LocalDate，前端传 yyyy-MM-dd；SQL 中匹配 create_time >= 当天 00:00） */
    private LocalDate startDate;

    /** 发布时间止（LocalDate；SQL 中 create_time < 次日 00:00，包含当天） */
    private LocalDate endDate;

    public ResourceQueryRequest() {
    }

    public void normalize() {
        if (pageNum == null || pageNum < 1) pageNum = Constants.DEFAULT_PAGE_NUM;
        if (pageSize == null || pageSize < 1) pageSize = Constants.DEFAULT_PAGE_SIZE;
        if (pageSize > Constants.MAX_PAGE_SIZE) pageSize = Constants.MAX_PAGE_SIZE;
        if (keyword != null) keyword = keyword.trim().isEmpty() ? null : keyword.trim();
        if (course != null) course = course.trim().isEmpty() ? null : course.trim();
    }

    public int getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }

    public Integer getPageNum() { return pageNum == null ? Constants.DEFAULT_PAGE_NUM : pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize == null ? Constants.DEFAULT_PAGE_SIZE : pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
