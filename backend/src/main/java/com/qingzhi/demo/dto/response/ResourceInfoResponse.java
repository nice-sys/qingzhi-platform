package com.qingzhi.demo.dto.response;

import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ReviewStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源信息响应 DTO
 * <p>对应 PRD 2.3.1 资源字段定义；返回给前端时省略敏感字段（如 fileStorage 的本地绝对路径可按需要脱敏）</p>
 */
public class ResourceInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String course;

    /** 上传者用户ID */
    private Long uploaderId;
    /** 上传者姓名（关联 user 表冗余展示） */
    private String uploaderName;

    private String fileName;
    private String fileExt;
    /** 文件大小（字节），前端格式化为 KB/MB */
    private Long fileSize;

    /** 下载次数 */
    private Integer downloadCount;

    /** 审核状态编码：0-待审核，1-已通过，2-已拒绝 */
    private Integer reviewStatus;
    /** 审核状态中文描述 */
    private String reviewStatusName;

    /** 拒绝理由（仅资源所有者 & 管理员可见；此处原样返回，由接口层判断） */
    private String rejectReason;
    /** 审核管理员用户ID */
    private Long reviewAdminId;
    /** 审核时间 */
    private LocalDateTime reviewTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ResourceInfoResponse() {
    }

    /**
     * 从实体转换（不填充 uploaderName，如需展示需二次关联）
     */
    public static ResourceInfoResponse fromEntity(Resource entity) {
        ResourceInfoResponse r = new ResourceInfoResponse();
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
        ReviewStatusEnum statusEnum = ReviewStatusEnum.of(entity.getReviewStatus());
        r.setReviewStatusName(statusEnum == null ? null : statusEnum.getMessage());
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
