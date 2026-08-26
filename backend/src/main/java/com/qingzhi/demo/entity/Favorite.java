package com.qingzhi.demo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏实体类
 * <p>对应 PRD 4.3 收藏表（favorite）</p>
 */
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（BIGINT, PK, AUTO_INCREMENT）
     */
    private Long id;

    /**
     * 用户ID（BIGINT, FK -> user.id, NOT NULL）
     */
    private Long userId;

    /**
     * 资源ID（BIGINT, FK -> resource.id, NOT NULL）
     */
    private Long resourceId;

    /**
     * 收藏时间（DATETIME, NOT NULL）
     */
    private LocalDateTime createTime;

    public Favorite() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
