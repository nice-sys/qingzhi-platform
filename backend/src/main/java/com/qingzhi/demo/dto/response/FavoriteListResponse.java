package com.qingzhi.demo.dto.response;

import com.qingzhi.demo.entity.Favorite;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ReviewStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的收藏列表项响应 DTO
 * <p>将「收藏记录 Favorite」 + 「资源 Resource」字段扁平化合并，方便前端直接渲染。
 * 注：若资源已被物理删除，则 resource 相关字段为 null，前端可展示为「该资源已下架/删除」占位。
 */
public class FavoriteListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ======== 收藏维度 ======== */

    /** 收藏记录主键 */
    private Long favoriteId;
    /** 收藏时间 */
    private LocalDateTime favoriteTime;

    /* ======== 资源维度（与 ResourceListResponse 对齐） ======== */

    private Long id;            // resource.id
    private String title;
    private String description;
    private String course;

    private Long uploaderId;
    private String uploaderName;

    private String fileName;
    private String fileExt;
    private Long fileSize;

    private Integer downloadCount;

    private Integer reviewStatus;
    private String reviewStatusName;

    private LocalDateTime createTime;   // resource.createTime
    private LocalDateTime updateTime;   // resource.updateTime

    public FavoriteListResponse() {
    }

    /**
     * 从 Favorite + Resource 两个实体构造
     *
     * @param fav      收藏记录（必填）
     * @param resource 关联的资源（允许 null，已删除的资源会是 null）
     */
    public static FavoriteListResponse fromEntities(Favorite fav, Resource resource) {
        if (fav == null) {
            return null;
        }
        FavoriteListResponse r = new FavoriteListResponse();
        r.setFavoriteId(fav.getId());
        r.setFavoriteTime(fav.getCreateTime());

        if (resource != null) {
            r.setId(resource.getId());
            r.setTitle(resource.getTitle());
            r.setDescription(resource.getDescription());
            r.setCourse(resource.getCourse());
            r.setUploaderId(resource.getUploaderId());
            r.setFileName(resource.getFileName());
            r.setFileExt(resource.getFileExt());
            r.setFileSize(resource.getFileSize());
            r.setDownloadCount(resource.getDownloadCount() == null ? 0 : resource.getDownloadCount());
            r.setReviewStatus(resource.getReviewStatus());
            ReviewStatusEnum s = ReviewStatusEnum.of(resource.getReviewStatus());
            r.setReviewStatusName(s == null ? null : s.getMessage());
            r.setCreateTime(resource.getCreateTime());
            r.setUpdateTime(resource.getUpdateTime());
        }
        return r;
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

    public Long getFavoriteId() { return favoriteId; }
    public void setFavoriteId(Long favoriteId) { this.favoriteId = favoriteId; }

    public LocalDateTime getFavoriteTime() { return favoriteTime; }
    public void setFavoriteTime(LocalDateTime favoriteTime) { this.favoriteTime = favoriteTime; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewStatusName() { return reviewStatusName; }
    public void setReviewStatusName(String reviewStatusName) { this.reviewStatusName = reviewStatusName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
