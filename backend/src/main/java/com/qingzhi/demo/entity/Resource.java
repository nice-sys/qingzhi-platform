package com.qingzhi.demo.entity;

import com.qingzhi.demo.enums.ReviewStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源实体类
 * <p>对应 PRD 2.3 资源管理模块 & PRD 4.2 资源表（resource）</p>
 */
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（BIGINT PK AUTO_INCREMENT）
     */
    private Long id;

    /**
     * 资源标题（VARCHAR 必填）
     */
    private String title;

    /**
     * 资源描述（TEXT，选填）
     */
    private String description;

    /**
     * 所属课程（VARCHAR 必填，如"软件工程"、"数据结构"）
     */
    private String course;

    /**
     * 上传者用户ID（BIGINT，关联 user.id，外键）
     */
    private Long uploaderId;

    /**
     * 文件名（用户上传时的原文件名，带扩展名）
     */
    private String fileName;

    /**
     * 文件存储路径（服务器本地路径或对象存储 key）
     */
    private String filePath;

    /**
     * 文件大小（字节数）
     */
    private Long fileSize;

    /**
     * 文件扩展名（pdf/docx/...，便于校验与预览识别）
     */
    private String fileExt;

    /**
     * 文件哈希（如 MD5 / SHA256；加分项「秒传」用）
     */
    private String fileHash;

    /**
     * 下载次数（默认 0）
     */
    private Integer downloadCount;

    /**
     * 审核状态（TINYINT）：0-待审核，1-已通过，2-已拒绝
     * <p>对应 {@link ReviewStatusEnum}</p>
     */
    private Integer reviewStatus;

    /**
     * 审核拒绝理由（仅当 reviewStatus=2 时有值；拒绝时必填）
     */
    private String rejectReason;

    /**
     * 审核管理员用户ID（谁审核通过/拒绝的）
     */
    private Long reviewAdminId;

    /**
     * 审核时间（通过或拒绝时写入）
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间（用户提交时间）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（用户修改 / 管理员审核时刷新）
     */
    private LocalDateTime updateTime;

    public Resource() {
    }

    /* ====================================================================================
     * 便捷方法
     * ==================================================================================== */

    public ReviewStatusEnum getReviewStatusEnum() {
        return ReviewStatusEnum.of(this.reviewStatus);
    }

    public void setReviewStatusEnum(ReviewStatusEnum e) {
        this.reviewStatus = e == null ? null : e.getCode();
    }

    /** 是否待审核状态 */
    public boolean isPending() {
        return ReviewStatusEnum.PENDING.getCode() == (reviewStatus == null ? -1 : reviewStatus);
    }

    /** 是否已通过审核 */
    public boolean isApproved() {
        return ReviewStatusEnum.APPROVED.getCode() == (reviewStatus == null ? -1 : reviewStatus);
    }

    /** 是否已拒绝 */
    public boolean isRejected() {
        return ReviewStatusEnum.REJECTED.getCode() == (reviewStatus == null ? -1 : reviewStatus);
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

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }

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
