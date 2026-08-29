package com.qingzhi.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日上传计数实体
 * <p>对应 daily_upload_count 表：草稿 + 正式资源 合并计数，每个用户每天最多 100 条 resource 行。
 * <p>使用 UNIQUE(user_id, upload_date) 保证幂等；配合 SELECT ... FOR UPDATE 行锁保证并发不穿数。
 *
 * @see com.qingzhi.demo.common.Constants#DAILY_UPLOAD_MAX_COUNT
 */
public class DailyUploadCount {

    private Long id;

    private Long userId;

    private LocalDate uploadDate;

    private Integer uploadCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }

    public Integer getUploadCount() { return uploadCount; }
    public void setUploadCount(Integer uploadCount) { this.uploadCount = uploadCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
