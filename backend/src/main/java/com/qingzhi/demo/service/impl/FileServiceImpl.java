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
    private volatile String uploadBaseDirRaw;

    /**
     * 真实使用的绝对路径根目录（@PostConstruct 中解析计算，彻底消除 CWD 差异）
     */
    private volatile String uploadBaseDir;

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

        // 4. 秒传命中判断：必须满足「DB 有相同哈希 + 对应磁盘文件真实存在 + 磁盘大小与 DB 记录一致」三重校验
        //    防止用户手动清空 uploads 目录后，仅命中 DB hash 就「秒传成功，实际无文件可下载」的脏问题
        FileStorage existing = fileStorageMapper.selectByFileHash(fileHash);
        boolean reallyHitQuickUpload = false;
        if (existing != null && existing.getId() != null) {
            final String realBaseDir = getResolvedUploadBaseDir();
            boolean diskOk = false;
            Path existAbs = null;
            String whyDiskBad = null;
            try {
                String relPath = (existing.getFilePath() == null) ? "" : existing.getFilePath().replace('\\', '/');
                if (!relPath.isEmpty()) {
                    existAbs = FileUtil.resolveAbsolutePath(realBaseDir, relPath);
                    if (!Files.exists(existAbs) || !Files.isRegularFile(existAbs)) {
                        whyDiskBad = "磁盘文件不存在（DB.filePath=" + relPath + " -> abs=" + existAbs + "）";
                    } else {
                        long diskSize = Files.size(existAbs);
                        Long dbSize = existing.getFileSize();
                        if (dbSize == null || diskSize != dbSize.longValue()) {
                            whyDiskBad = "磁盘大小与 DB 记录不一致（DB.fileSize=" + dbSize + " vs disk=" + diskSize + "，abs=" + existAbs + "）";
                        } else {
                            diskOk = true;
                        }
                    }
                } else {
                    whyDiskBad = "DB.filePath 为空（脏数据）";
                }
            } catch (Exception e) {
                whyDiskBad = "磁盘校验异常：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                log.warn("[秒传-磁盘校验异常] fileHash={}, fileStorageId={}", fileHash, existing.getId(), e);
            }

            if (diskOk) {
                // 4a. 真秒传命中：引用计数 +1，不写磁盘，直接返回
                fileStorageMapper.incrementReferenceCount(existing.getId());
                existing.setOriginalFileName(originalName);
                String ext = FileUtil.getExtension(originalName);
                if (ext != null && !ext.isEmpty()) existing.setFileExt(ext);
                FileStorage updated = fileStorageMapper.selectById(existing.getId());
                log.info("[秒传命中-OK] hash={}, fileStorageId={}, absPath={}, 原引用={}, 新引用={}",
                        fileHash, existing.getId(), existAbs,
                        existing.getReferenceCount(), (updated == null ? null : updated.getReferenceCount()));
                reallyHitQuickUpload = true;
                return buildResult(existing, originalName, true);
            } else {
                // 4b. DB 命中哈希但磁盘丢失/损坏：打印 WARN 降级为「正常上传 + 把死 DB 记录校正为新的路径/大小/ref=1」
                //    否则下次再上传同一哈希还会命中这条死记录
                log.warn("[秒传命中但磁盘丢失/损坏，降级为正常上传+校正脏DB] hash={}, fileStorageId={}, why={}",
                        fileHash, existing.getId(), whyDiskBad);
                // 继续走下面真实写盘流程；写盘成功后用 updatePathAndSize 把 existing.id 这条死记录覆盖回来，
                // 避免新 insert 时因为 file_hash UNIQUE 约束报 Duplicate key
            }
        }

        // 5. 真实写盘（未命中哈希 或 哈希命中但磁盘已损坏/丢失 都走到这里）
        String relativePath = FileUtil.generateRelativePath(originalName);
        final String realBaseDir = getResolvedUploadBaseDir();
        Path absPath = null;
        try {
            absPath = FileUtil.resolveAbsolutePath(realBaseDir, relativePath);
            // 诊断日志（写盘前）：明确告诉我们「用了哪个 baseDir → 最终写到哪个磁盘路径」
            log.warn("[上传-写盘诊断] uploaderId={}, fileName={}, realBaseDir={}, absPath={}, bytesLen={}",
                    uploaderId, originalName, realBaseDir, absPath, fileBytes.length);

            // 二次强制父目录（resolveAbsolutePath 已做过，兜底 Windows 权限问题）
            Path parent = absPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("[上传-写盘] 补建父目录：{}", parent);
            }
            long written;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
                written = FileUtil.saveStream(bis, absPath);
            }
            // ✅ 写盘后强验证：文件必须真实存在 + 大小必须等于上传字节数，否则强制报错（杜绝假成功）
            boolean exists = Files.exists(absPath);
            long sizeOnDisk = exists ? Files.size(absPath) : -1L;
            if (!exists || sizeOnDisk != fileBytes.length) {
                log.error("[上传-写盘失败] 文件未真实落盘！absPath={}, exists={}, sizeOnDisk={}, expected={}",
                        absPath, exists, sizeOnDisk, fileBytes.length);
                BusinessException.throwOf(ResponseCodeEnum.FILE_UPLOAD_FAILED,
                        "文件写入服务器失败（已校验落盘存在性与大小不匹配）");
            }
            log.info("[上传-写盘成功] absPath={}, written={}, sizeOnDisk={}", absPath, written, sizeOnDisk);

            // 🚨 异步二次校验（不阻塞上传接口）：写盘后 2 秒再检查一次文件是否还存在
            // → 若 2 秒后不存在：100% 是 Windows Defender/杀毒软件把文件隔离删除了
            final Path checkPath = absPath;
            final long expectedSize = fileBytes.length;
            final String diagName = originalName;
            final Long fid = uploaderId;
            java.lang.Thread checker = new java.lang.Thread(() -> {
                try { Thread.sleep(2000L); } catch (InterruptedException _e) { Thread.currentThread().interrupt(); return; }
                try {
                    boolean still = Files.exists(checkPath);
                    long sz = still ? Files.size(checkPath) : -1L;
                    if (!still || sz != expectedSize) {
                        log.error("[上传-2秒后文件消失！疑似安全软件隔离] uploaderId={}, fileName={}, absPath={} " +
                                        "\n  刚写完时 size={}，2 秒后 exists={}, sizeOnDisk={}. " +
                                        "\n  【建议操作】：请在 Windows 安全中心 → 病毒和威胁防护 → 保护历史记录中，" +
                                        "将 {} 文件夹添加为「排除项」，或将被隔离的文件恢复；或临时关闭实时保护后重新上传。",
                                fid, diagName, checkPath, expectedSize, still, sz,
                                getResolvedUploadBaseDir());
                    } else {
                        log.debug("[上传-2秒后存活] 文件仍在磁盘：{}", checkPath);
                    }
                } catch (Exception e) {
                    log.warn("[上传-2秒后存活检查异常] absPath={}", checkPath, e);
                }
            }, "UploadFileChecker-" + (checkPath.getFileName() == null ? "" : checkPath.getFileName()));
            checker.setDaemon(true);
            checker.start();
        } catch (IOException e) {
            log.error("保存上传文件到磁盘失败 [uploaderId={}, fileName={}, path={}, absPath={}]",
                    uploaderId, originalName, relativePath, absPath, e);
            BusinessException.throwOf(ResponseCodeEnum.FILE_UPLOAD_FAILED,
                    "保存上传文件失败（" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()) + "）");
        }

        FileStorage storage;
        if (existing != null && existing.getId() != null && !reallyHitQuickUpload) {
            // 走到这里 = 之前 DB 有相同 hash 但磁盘已丢/损坏 → 用 updatePathAndSize 校正回这条原记录，
            // 避免 INSERT 时撞 file_hash UNIQUE 约束
            int rows = fileStorageMapper.updatePathAndSize(existing.getId(), relativePath, fileSize, 1);
            log.info("[上传-脏DB校正] fileHash={}, fileStorageId={}, newPath={}, newSize={}, rows={}",
                    fileHash, existing.getId(), relativePath, fileSize, rows);
            storage = fileStorageMapper.selectById(existing.getId());
            if (storage != null) {
                storage.setOriginalFileName(originalName);
                String ext = FileUtil.getExtension(originalName);
                if (ext != null && !ext.isEmpty()) storage.setFileExt(ext);
            } else {
                storage = new FileStorage();
                storage.setId(existing.getId());
                storage.setFileHash(fileHash);
                storage.setFilePath(relativePath);
                storage.setFileSize(fileSize);
                storage.setReferenceCount(1);
                storage.setOriginalFileName(originalName);
                String ext = FileUtil.getExtension(originalName);
                if (ext != null && !ext.isEmpty()) storage.setFileExt(ext);
            }
        } else {
            // 正常首次上传新文件
            storage = new FileStorage();
            storage.setFileHash(fileHash);
            storage.setFilePath(relativePath);
            storage.setFileSize(fileSize);
            storage.setReferenceCount(1);
            storage.setOriginalFileName(originalName);
            String ext = FileUtil.getExtension(originalName);
            if (ext != null && !ext.isEmpty()) storage.setFileExt(ext);
            fileStorageMapper.insert(storage);
        }

        log.info("文件上传完成（{}）：fileStorageId={}, hash={}, path={}, size={}",
                (reallyHitQuickUpload ? "真正秒传" : (existing == null ? "新文件上传" : "DB脏记录校正后写入")),
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
    public String getUploadBaseDirDebug() { return getResolvedUploadBaseDir(); }

    /**
     * 保证永远返回非空的绝对路径根目录
     * <p>即使 @PostConstruct init() 因 Bean 生命周期问题没执行，也 fallback 计算一次。</p>
     */
    private String getResolvedUploadBaseDir() {
        String cur = this.uploadBaseDir;
        if (cur != null && !cur.isEmpty()) return cur;
        synchronized (this) {
            cur = this.uploadBaseDir;
            if (cur != null && !cur.isEmpty()) return cur;
            String raw = (this.uploadBaseDirRaw == null) ? "./uploads" : this.uploadBaseDirRaw.trim();
            Path p = Paths.get(raw);
            if (!p.isAbsolute()) {
                String anchor = System.getenv("QINGZHI_PROJECT_ROOT");
                if (anchor == null || anchor.isEmpty()) {
                    String userDir = System.getProperty("user.dir");
                    if (userDir != null && !userDir.isEmpty()) {
                        Path ud = Paths.get(userDir);
                        if (ud.getFileName() != null && "backend".equalsIgnoreCase(ud.getFileName().toString())) {
                            Path parent = ud.getParent();
                            anchor = (parent != null) ? parent.toString() : userDir;
                        } else {
                            anchor = userDir;
                        }
                    } else {
                        anchor = ".";
                    }
                }
                p = Paths.get(anchor, raw).normalize().toAbsolutePath();
            }
            cur = p.toString();
            this.uploadBaseDir = cur;
            log.warn("[文件存储] fallback 初始化根目录：raw={} → abs={}", raw, cur);
            try { if (!Files.exists(p)) Files.createDirectories(p); }
            catch (IOException e) { log.warn("[文件存储] fallback 创建目录失败：{}", cur, e); }
            return cur;
        }
    }

    @Override
    public Path resolveFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        try {
            final String realBaseDir = getResolvedUploadBaseDir();
            final String normalized = relativePath.replace('\\', '/');

            // 候选 1（关键！与上传时 100% 同一 API：FileUtil.resolveAbsolutePath）
            Path abs = FileUtil.resolveAbsolutePath(realBaseDir, normalized);
            if (Files.exists(abs) && Files.isRegularFile(abs)) {
                log.debug("[resolveFile] 命中候选1(标准方法) path={}", abs);
                return abs;
            }

            // 候选 2：去掉开头 "uploads/" 前缀（历史数据可能多一层）
            if (normalized.startsWith("uploads/")) {
                Path abs2 = FileUtil.resolveAbsolutePath(realBaseDir, normalized.substring("uploads/".length()));
                if (Files.exists(abs2) && Files.isRegularFile(abs2)) {
                    log.debug("[resolveFile] 命中候选2(去掉uploads前缀) path={}", abs2);
                    return abs2;
                }
                // 详细诊断：父目录是否存在 + 同目录下是否有其他文件（区分「整个目录消失 vs 单个文件被隔离」）
                Path parent = abs2.getParent();
                String prefix = extractFilePrefix(normalized);
                int similar = countSimilar(parent, prefix);
                int total  = countTotal(parent);
                log.warn("[resolveFile] 都未命中(含前缀诊断) path={} \n  候选1(FileUtil法) = {} exists={} \n  候选2(去前缀)   = {} exists={} \n  父目录 {} exists={}  totalFiles={}  uuid前缀匹配数={}",
                        relativePath, abs, Files.exists(abs), abs2, Files.exists(abs2),
                        parent, (parent != null && Files.exists(parent)), total, similar);
                return null;
            }

            // 候选 3：normalized 本身是绝对路径（Windows 盘符开头 D:/ 或 Linux /）
            boolean looksAbs = normalized.length() > 2 && normalized.charAt(1) == ':'
                    || normalized.startsWith("/");
            if (looksAbs) {
                try {
                    Path absOnly = Paths.get(normalized).normalize().toAbsolutePath();
                    if (Files.exists(absOnly) && Files.isRegularFile(absOnly)) {
                        log.debug("[resolveFile] 命中候选3(本身绝对路径) path={}", absOnly);
                        return absOnly;
                    }
                } catch (Exception _ignore) { /* 非法绝对路径格式跳过 */ }
            }

            // 通用详细诊断（normalized 不含 uploads/ 前缀时）
            Path parent = abs.getParent();
            String prefix = extractFilePrefix(normalized);
            int similar = countSimilar(parent, prefix);
            int total  = countTotal(parent);
            log.warn("[resolveFile] 未命中(含详细诊断) path={} \n  候选1(FileUtil法) = {} exists={} \n  父目录 {} exists={}  totalFiles={}  uuid前缀匹配数={}",
                    relativePath, abs, Files.exists(abs),
                    parent, (parent != null && Files.exists(parent)), total, similar);
            return null;
        } catch (Exception e) {
            log.error("解析文件路径异常：{}", relativePath, e);
        }
        return null;
    }

    /** 从相对路径末尾提取文件名（不含扩展名）的前 8 位，用于安全软件隔离后的相似匹配诊断 */
    private static String extractFilePrefix(String normalized) {
        if (normalized == null) return "";
        int slash = normalized.lastIndexOf('/');
        String name = (slash < 0) ? normalized : normalized.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        String base = (dot < 0) ? name : name.substring(0, dot);
        return base.length() > 8 ? base.substring(0, 8) : base;
    }
    private static int countTotal(Path parent) {
        if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent)) return -1;
        try (var s = Files.list(parent)) { return (int) s.count(); } catch (IOException e) { return -2; }
    }
    private static int countSimilar(Path parent, String prefix) {
        if (parent == null || prefix == null || prefix.isEmpty()
                || !Files.exists(parent) || !Files.isDirectory(parent)) return -1;
        try (var s = Files.list(parent)) {
            return (int) s.filter(p -> {
                String n = p.getFileName() == null ? "" : p.getFileName().toString();
                return n.startsWith(prefix);
            }).count();
        } catch (IOException e) { return -2; }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseReference(Long fileStorageId) {
        if (fileStorageId == null || fileStorageId <= 0) return;
        FileStorage existing = fileStorageMapper.selectById(fileStorageId);
        if (existing == null) {
            log.warn("[increaseReference] fileStorageId 不存在，忽略：{}", fileStorageId);
            return;
        }
        int rows = fileStorageMapper.incrementReferenceCount(fileStorageId);
        FileStorage updated = fileStorageMapper.selectById(fileStorageId);
        log.info("[increaseReference] fileStorageId={}, 原引用={}, 新引用={}, rows={}",
                fileStorageId, existing.getReferenceCount(),
                updated == null ? null : updated.getReferenceCount(), rows);
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
