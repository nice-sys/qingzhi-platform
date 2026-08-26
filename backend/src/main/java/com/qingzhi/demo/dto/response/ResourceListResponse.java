package com.qingzhi.demo.dto.response;

import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ReviewStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源列表项响应 DTO
 * <p>用于公开列表 / 我的资源 / 管理员审核列表等；相比详情版略精简（去掉了拒绝理由细节等，需要时可从详情接口拉取）</p>
 */
public class ResourceListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String course;

    /** 上传者用户ID */
    private Long uploaderId;
    /** 上传者姓名（关联 user 表填充） */
    private String uploaderName;

    private String fileName;
    private String fileExt;
    /** 文件大小（字节） */
    private Long fileSize;

    /** 下载次数 */
    private Integer downloadCount;

    /** 审核状态：0-待审核 1-已通过 2-已拒绝 */
    private Integer reviewStatus;
    /** 审核状态中文名 */
    private String reviewStatusName;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ResourceListResponse() {
    }

    /**
     * 从 Resource 实体构造（不填充 uploaderName）
     */
    public static ResourceListResponse fromEntity(Resource entity) {
        if (entity == null) {
            return null;
        }
        ResourceListResponse r = new ResourceListResponse();
        r.setId(entity.getId());
        r.setTitle(entity.getTitle());
        r.setDescription(entity.getDescription());
        r.setCourse(entity.getCourse());
        r.setUploaderId(entity.getUploaderId());
        r.setFileName(entity.getFileName());
        r.setFileExt(entity.getFileExt());
        r.setFileSize(entity.getFileSize());
        r.setDownloadCount(entity.getDownloadCount() == null ? 0 : entity.getDownloadCount());
        r.setReviewStatus(entity.getReviewStatus());
        ReviewStatusEnum s = ReviewStatusEnum.of(entity.getReviewStatus());
        r.setReviewStatusName(s == null ? null : s.getMessage());
        r.setCreateTime(entity.getCreateTime());
        r.setUpdateTime(entity.getUpdateTime());
        return r;
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

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
