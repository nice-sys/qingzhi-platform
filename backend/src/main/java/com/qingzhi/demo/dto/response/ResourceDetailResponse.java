package com.qingzhi.demo.dto.response;

import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ReviewStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源详情响应 DTO
 * <p>用于 GET /api/resource/{id} 详情页；字段上与列表版一致，便于前端按需扩展（预览地址等后续可追加）</p>
 */
public class ResourceDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String course;

    /** 上传者用户ID */
    private Long uploaderId;
    /** 上传者姓名（关联 user 表填充） */
    private String uploaderName;
    /** 上传者账号（学号/工号，便于展示） */
    private String uploaderUsername;

    private String fileName;
    private String fileExt;
    /** 文件大小（字节），前端格式化为 KB/MB */
    private Long fileSize;

    /** 下载次数 */
    private Integer downloadCount;

    /** 审核状态编码：0-待审核 1-已通过 2-已拒绝 */
    private Integer reviewStatus;
    /** 审核状态中文描述 */
    private String reviewStatusName;

    /** 拒绝理由（仅资源所有者 & 管理员可见，Controller 层做脱敏） */
    private String rejectReason;
    /** 审核管理员用户ID */
    private Long reviewAdminId;
    /** 审核时间 */
    private LocalDateTime reviewTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ResourceDetailResponse() {
    }

    /**
     * 从 Resource 实体构造（不含 uploader 关联字段，需二次填充）
     */
    public static ResourceDetailResponse fromEntity(Resource entity) {
        if (entity == null) {
            return null;
        }
        ResourceDetailResponse r = new ResourceDetailResponse();
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
        r.setRejectReason(entity.getRejectReason());
        r.setReviewAdminId(entity.getReviewAdminId());
        r.setReviewTime(entity.getReviewTime());
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

    public String getUploaderUsername() { return uploaderUsername; }
    public void setUploaderUsername(String uploaderUsername) { this.uploaderUsername = uploaderUsername; }

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

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Long getReviewAdminId() { return reviewAdminId; }
    public void setReviewAdminId(Long reviewAdminId) { this.reviewAdminId = reviewAdminId; }

    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
