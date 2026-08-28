package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.entity.FileStorage;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.FileStorageMapper;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.utils.FileUtil;
import com.qingzhi.demo.utils.HashUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件服务实现（含秒传加分项）
 * <p>对应 PRD 2.3.1 文件上传/下载 + 加分项秒传 + 3.1 性能需求。</p>
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileStorageMapper fileStorageMapper;

    /**
     * 本地存储根目录（原始注入值，可能是相对路径）
     */
    @Value("${qingzhi.upload.base-dir:./uploads}")
    private String uploadBaseDirRaw;

    /**
     * 真实使用的绝对路径根目录（@PostConstruct 中解析计算，彻底消除 CWD 差异）
     */
    private String uploadBaseDir;

    public FileServiceImpl(FileStorageMapper fileStorageMapper) {
        this.fileStorageMapper = fileStorageMapper;
    }

    @PostConstruct
    public void init() {
        String raw = uploadBaseDirRaw == null ? "./uploads" : uploadBaseDirRaw.trim();
        Path p = Paths.get(raw);
        if (!p.isAbsolute()) {
            // 相对路径：优先以「项目根目录」作为锚点（D:\QingZhi），其次 user.dir（JVM 启动目录）
            String anchor = System.getenv("QINGZHI_PROJECT_ROOT");
            if (anchor == null || anchor.isEmpty()) {
                String userDir = System.getProperty("user.dir");
                if (userDir != null && !userDir.isEmpty()) {
                    // 若是 backend 子目录启动，退回父目录（D:\QingZhi\backend -> D:\QingZhi）
                    Path ud = Paths.get(userDir);
                    if (ud.getFileName() != null && "backend".equalsIgnoreCase(ud.getFileName().toString())) {
                        Path parent = ud.getParent();
                        if (parent != null) anchor = parent.toString();
                        else anchor = userDir;
                    } else {
                        anchor = userDir;
                    }
                } else {
                    anchor = ".";
                }
            }
            p = Paths.get(anchor, raw).normalize().toAbsolutePath();
        }
        this.uploadBaseDir = p.toString();
        // 启动时打一条日志，便于核对"上传/下载"解析位置一致
        log.info("[文件存储] 根目录解析：原始配置={} → 绝对路径={}", uploadBaseDirRaw, this.uploadBaseDir);
        try {
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (IOException e) {
            log.warn("[文件存储] 创建存储目录失败（不影响启动）：{}", this.uploadBaseDir, e);
        }
    }

    /* ====================================================================================
     * 一、上传文件（含秒传）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadFile(MultipartFile file, Long uploaderId) {
        // 1. 基础校验
        BusinessException.throwIf(file == null || file.isEmpty(),
                ResponseCodeEnum.FILE_UPLOAD_FAILED, "上传文件为空");

        String originalName = file.getOriginalFilename();
        BusinessException.throwIf(originalName == null || originalName.isEmpty(),
                ResponseCodeEnum.FILE_UPLOAD_FAILED, "无法获取上传文件名");

        long fileSize = file.getSize();
        FileUtil.validateFileSize(fileSize);
        FileUtil.validateExtension(originalName);

        // 2. 先将上传流读到 byte[]，以便既能计算哈希，又能二次写盘
        //    （HashUtil.md5Stream 会消费 InputStream，所以我们读入内存后再复用）
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败 [uploaderId={}, fileName={}]", uploaderId, originalName, e);
            BusinessException.throwOf(ResponseCodeEnum.FILE_UPLOAD_FAILED, "读取上传文件失败");
            return new HashMap<>();
        }

        // 3. 计算 MD5 哈希（秒传判断）
        String fileHash = HashUtil.md5(fileBytes);
        BusinessException.throwIf(fileHash == null || fileHash.isEmpty(),
                ResponseCodeEnum.FILE_UPLOAD_FAILED, "文件哈希计算失败");

        // 4. 秒传命中判断
        FileStorage existing = fileStorageMapper.selectByFileHash(fileHash);
        if (existing != null) {
            // 4a. 秒传命中：引用计数 +1，不写磁盘，直接返回
            fileStorageMapper.incrementReferenceCount(existing.getId());
            // 无论 DB 是否有这两列，都写实体引用计数用不到；但 fromEntity/回填逻辑需要（未来加列自动生效）
            existing.setOriginalFileName(originalName);
            String ext = FileUtil.getExtension(originalName);
            if (ext != null && !ext.isEmpty()) existing.setFileExt(ext);
            log.info("秒传命中：哈希 {} 已存在，引用计数已 +1（当前引用数={})",
                    fileHash, existing.getReferenceCount() + 1);
            return buildResult(existing, originalName, true);
        }

        // 4b. 未命中：真实写盘 + 新增 file_storage 记录
        String relativePath = FileUtil.generateRelativePath(originalName);
        try {
            Path absPath = FileUtil.resolveAbsolutePath(uploadBaseDir, relativePath);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
                FileUtil.saveStream(bis, absPath);
            }
        } catch (IOException e) {
            log.error("保存上传文件到磁盘失败 [uploaderId={}, fileName={}, path={}]",
                    uploaderId, originalName, relativePath, e);
            BusinessException.throwOf(ResponseCodeEnum.FILE_UPLOAD_FAILED, "保存上传文件失败");
        }

        FileStorage storage = new FileStorage();
        storage.setFileHash(fileHash);
        storage.setFilePath(relativePath);
        storage.setFileSize(fileSize);
        storage.setReferenceCount(1);
        storage.setOriginalFileName(originalName);
        String ext = FileUtil.getExtension(originalName);
        if (ext != null && !ext.isEmpty()) storage.setFileExt(ext);
        fileStorageMapper.insert(storage);

        log.info("文件上传完成（新文件）：fileStorageId={}, hash={}, path={}, size={}",
                storage.getId(), fileHash, relativePath, fileSize);

        return buildResult(storage, originalName, false);
    }

    /* ====================================================================================
     * 二、下载 / 查询 / 释放引用
     * ==================================================================================== */

    @Override
    public FileStorage getFileStorageById(Long fileStorageId) {
        if (fileStorageId == null) return null;
        return fileStorageMapper.selectById(fileStorageId);
    }

    @Override
    public FileStorage getFileStorageByHash(String fileHash) {
        if (fileHash == null || fileHash.isEmpty()) return null;
        return fileStorageMapper.selectByFileHash(fileHash);
    }

    @Override
    public String getUploadBaseDirDebug() { return uploadBaseDir; }

    @Override
    public Path resolveFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        try {
            // 候选 1：直接 baseDir + relativePath（大多数情况）
            Path abs = Paths.get(uploadBaseDir, relativePath).normalize();
            if (Files.exists(abs) && Files.isRegularFile(abs)) {
                log.debug("[resolveFile] 命中候选1 path={}", abs);
                return abs;
            }
            // 候选 2：把 Windows 反斜杠 \ 统一转为 /（常见因 URL/表单拼接留下的混合）
            String normalized = relativePath.replace('\\', '/');
            if (!normalized.equals(relativePath)) {
                Path abs2 = Paths.get(uploadBaseDir, normalized).normalize();
                if (Files.exists(abs2) && Files.isRegularFile(abs2)) {
                    log.debug("[resolveFile] 命中候选2 path={}", abs2);
                    return abs2;
                }
                // 候选 3：去掉开头的 uploads/ 前缀（有的版本存储会多写一层）
                if (normalized.startsWith("uploads/")) {
                    Path abs3 = Paths.get(uploadBaseDir, normalized.substring("uploads/".length())).normalize();
                    if (Files.exists(abs3) && Files.isRegularFile(abs3)) {
                        log.debug("[resolveFile] 命中候选3 path={}", abs3);
                        return abs3;
                    }
                    log.warn("[resolveFile] 都未命中 path={} candidates=[{} , {} , {}]",
                            relativePath, abs, abs2, abs3);
                    return null;
                }
                log.warn("[resolveFile] 都未命中 path={} candidates=[{} , {}]",
                        relativePath, abs, abs2);
                return null;
            }
            // 候选 3（无前缀混合时）：相对路径是否以 / 开头？绝对路径兜底
            if (normalized.startsWith("/") && normalized.length() > 1) {
                try {
                    Path absOnly = Paths.get(normalized).normalize();
                    if (Files.exists(absOnly) && Files.isRegularFile(absOnly)) {
                        log.debug("[resolveFile] 命中候选3(绝对路径) path={}", absOnly);
                        return absOnly;
                    }
                } catch (Exception _ignore) { /* 不是合法绝对路径，跳过 */ }
            }
            log.warn("[resolveFile] 未命中 path={} candidate={}", relativePath, abs);
            return null;
        } catch (Exception e) {
            log.error("解析文件路径异常：{}", relativePath, e);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReference(Long fileStorageId) {
        if (fileStorageId == null) return;
        FileStorage storage = fileStorageMapper.selectById(fileStorageId);
        if (storage == null) return;

        // 1. 引用计数 -1
        fileStorageMapper.decrementReferenceCount(fileStorageId);

        // 2. 重新加载，确认引用是否归零
        FileStorage updated = fileStorageMapper.selectById(fileStorageId);
        if (updated != null && updated.getReferenceCount() != null && updated.getReferenceCount() <= 0) {
            // 计数归 0 → 删除 DB 记录 + 删除磁盘文件
            fileStorageMapper.deleteById(fileStorageId);
            if (updated.getFilePath() != null) {
                Path abs = Paths.get(uploadBaseDir, updated.getFilePath());
                boolean del = FileUtil.deleteFile(abs);
                log.info("文件引用归零，已清理 DB + 磁盘：fileStorageId={}, path={}, 磁盘删除={}",
                        fileStorageId, updated.getFilePath(), del);
            }
        }
    }

    /* ====================================================================================
     * 三、私有辅助
     * ==================================================================================== */

    private Map<String, Object> buildResult(FileStorage storage, String originalFileName, boolean hitQuickUpload) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("fileStorageId", storage.getId());
        res.put("fileHash", storage.getFileHash());
        res.put("fileName", originalFileName);
        res.put("storagePath", storage.getFilePath());
        res.put("fileSize", storage.getFileSize());
        res.put("fileExt", FileUtil.getExtension(originalFileName));
        res.put("hitQuickUpload", hitQuickUpload);
        return res;
    }
}
