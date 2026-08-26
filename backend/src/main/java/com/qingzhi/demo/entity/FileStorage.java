package com.qingzhi.demo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储实体类（加分项：秒传）
 * <p>对应 PRD 4.4 文件存储表（file_storage）。
 * <p>设计目的：上传文件时先计算 MD5/SHA-256 哈希值，与该表已有记录对比；
 * 若相同哈希已存在，直接复用已有文件（引用计数 +1），不重复写磁盘，实现「秒传」。
 */
public class FileStorage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（BIGINT, PK, AUTO_INCREMENT）
     */
    private Long id;

    /**
     * 文件哈希值（MD5 32位 或 SHA-256 64位；统一长度不超过 64）
     * <p>UNIQUE 约束，用于秒传命中判断。</p>
     */
    private String fileHash;

    /**
     * 实际存储路径（服务器本地相对路径或对象存储 key）
     * <p>例："2026/08/26/a1b2c3d4e5f6.pdf"</p>
     */
    private String filePath;

    /**
     * 文件大小（字节数）
     */
    private Long fileSize;

    /**
     * 引用计数：有多少个 Resource 记录引用了该文件
     * <p>新上传首次创建=1；每命中一次秒传+1；删除关联 resource 时-1；=0 则可删磁盘文件。</p>
     */
    private Integer referenceCount;

    /**
     * 上传时间（首次创建的时间；命中秒传时不改变）
     */
    private LocalDateTime createTime;

    public FileStorage() {
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

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(Integer referenceCount) {
        this.referenceCount = referenceCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
