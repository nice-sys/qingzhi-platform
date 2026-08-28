package com.qingzhi.demo.service;

import com.qingzhi.demo.entity.FileStorage;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * 文件服务接口
 * <p>对应 PRD 2.3.1 文件上传/下载 + 加分项秒传。</p>
 */
public interface FileService {

    /**
     * 上传文件（含加分项秒传逻辑）
     * <p>处理流程：
     * <ol>
     *   <li>校验：空文件 / 文件大小（<=50MB）/ 扩展名白名单</li>
     *   <li>计算 MD5 哈希（秒传用）</li>
     *   <li>查 file_storage 表：<br>
     *       · 命中 → 引用计数 +1，返回已有记录（秒传）<br>
     *       · 未命中 → 落盘 → 新增 file_storage 记录</li>
     * </ol>
     *
     * @param file      上传文件（MultipartFile）
     * @param uploaderId 上传者 userId（仅用于日志审计，可选，传 null 则不记录日志关联）
     * @return 上传结果 Map，结构：
     * <pre>{
     *   fileStorageId: Long,        // file_storage.id
     *   fileHash: String,           // MD5 32位
     *   fileName: String,           // 原始文件名
     *   storagePath: String,        // 相对存储路径（用于存 resource.file_path）
     *   fileSize: Long,             // 字节数
     *   fileExt: String,            // 扩展名（小写，无点）
     *   hitQuickUpload: Boolean     // true=命中秒传，false=真实落盘
     * }</pre>
     */
    Map<String, Object> uploadFile(MultipartFile file, Long uploaderId);

    /**
     * 根据 fileStorageId 查询 FileStorage（下载前校验 / Resource 发布时关联引用）
     *
     * @param fileStorageId file_storage.id
     * @return 不存在时返回 null
     */
    FileStorage getFileStorageById(Long fileStorageId);

    /**
     * 根据文件哈希查询已存储记录（秒传判断 / Resource 删除时释放引用用）
     *
     * @param fileHash 文件 MD5/SHA-256 哈希值
     * @return 不存在时返回 null
     */
    FileStorage getFileStorageByHash(String fileHash);

    /**
     * 根据相对存储路径解析为绝对磁盘路径（下载时使用）
     *
     * @param relativePath file_storage.file_path（或 resource.file_path）
     * @return 绝对 Path；不存在时返回 null
     */
    Path resolveFile(String relativePath);

    /**
     * 调试暴露当前 uploadBaseDir 配置值（日志打印候选路径时调用）
     * @return application.yml 配置的 base-dir 或默认 ./uploads
     */
    default String getUploadBaseDirDebug() { return "./uploads"; }

    /**
     * 引用计数 -1（Resource 被删除时调用，用于释放文件关联）
     * <p>当引用计数降为 0 时，物理删除磁盘文件 + DB 记录。</p>
     *
     * @param fileStorageId file_storage.id
     */
    void releaseReference(Long fileStorageId);
}
