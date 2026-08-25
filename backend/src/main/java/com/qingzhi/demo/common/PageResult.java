package com.qingzhi.demo.common;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应封装（列表数据 + 分页元信息）
 * <p>对应 PRD 3.1 性能需求：资源列表、收藏列表、个人资源列表均需支持分页</p>
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据列表 */
    private List<T> records;

    /** 当前页码（从 1 开始） */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Integer pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Integer pageNum, Integer pageSize, Long total) {
        this.records = records;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = calculatePages(pageSize, total);
    }

    /**
     * 构造分页结果（自动计算总页数）
     *
     * @param records  当前页数据
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param total    总记录数
     * @param <T>      元素类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> records, Integer pageNum, Integer pageSize, Long total) {
        return new PageResult<>(records, pageNum, pageSize, total);
    }

    /**
     * 构造空分页结果（用于无数据场景）
     *
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param <T>      元素类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResult<>(List.of(), pageNum, pageSize, 0L);
    }

    /**
     * 根据每页条数和总数计算总页数
     */
    private static Integer calculatePages(Integer pageSize, Long total) {
        if (pageSize == null || pageSize <= 0 || total == null || total <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNum != null && pages != null && pageNum < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum != null && pageNum > 1;
    }

    /**
     * 是否为空（当前页没有数据）
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        if (this.total != null) {
            this.pages = calculatePages(this.pageSize, this.total);
        }
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
        if (this.pageSize != null) {
            this.pages = calculatePages(this.pageSize, this.total);
        }
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }
}
