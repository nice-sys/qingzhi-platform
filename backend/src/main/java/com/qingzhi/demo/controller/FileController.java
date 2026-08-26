package com.qingzhi.demo.controller;

import com.qingzhi.demo.annotation.RateLimit;
import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.entity.FileStorage;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/**
 * 文件模块控制器
 * <p>对应 PRD 2.3.1 文件上传/下载 + 加分项秒传。</p>
 * <p>接口路径统一 /api/file，已在 WebConfig 注册 JwtInterceptor（除放行路径外需登录）。</p>
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /* ====================================================================================
     * 1. 文件上传（含加分项秒传）
     *    POST /api/file/upload
     *    Content-Type: multipart/form-data
     *    表单字段：file（必填，multipart）
     *    返回：Result<Map>  —— 包含 fileStorageId / fileHash / fileName / storagePath / fileSize / fileExt / hitQuickUpload
     * ==================================================================================== */

    @PostMapping("/upload")
    @RateLimit(dimension = RateLimit.LimitDimension.USER,
            windowSeconds = Constants.FILE_UPLOAD_RATE_WINDOW_SECONDS,
            maxRequests = Constants.FILE_UPLOAD_RATE_LIMIT)
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest request) {
        Long uploaderId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);
        Map<String, Object> uploadResult = fileService.uploadFile(file, uploaderId);
        return Result.success(uploadResult);
    }

    /* ====================================================================================
     * 2. 文件下载
     *    GET /api/file/download/{id}
     *    PathVariable: id = file_storage.id
     *    响应：application/octet-stream，浏览器触发下载
     * ==================================================================================== */

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id,
                                             HttpServletRequest request) {
        // 1. 登录态校验（JwtInterceptor 已注入当前用户，但这里兜底校验，防止匿名访问下载接口）
        Long currentUserId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIf(currentUserId == null,
                ResponseCodeEnum.UNAUTHORIZED);

        // 2. 校验 fileStorage 是否存在
        FileStorage storage = fileService.getFileStorageById(id);
        BusinessException.throwIfNull(storage,
                ResponseCodeEnum.FILE_NOT_FOUND, "文件不存在或已被删除");

        // 3. 解析磁盘绝对路径并校验文件存在性
        Path absPath = fileService.resolveFile(storage.getFilePath());
        BusinessException.throwIfNull(absPath,
                ResponseCodeEnum.FILE_NOT_FOUND, "文件磁盘记录缺失，请联系管理员");

        // 4. 组装下载响应（Content-Disposition 支持中文文件名）
        Resource resource = new FileSystemResource(absPath.toFile());

        String originalName = extractDownloadFileName(storage, absPath);
        String encoded;
        try {
            encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "download";
        }
        String contentDisposition = "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;

        String contentType = guessMediaType(originalName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION)
                .contentLength(storage.getFileSize() != null ? storage.getFileSize() : absPath.toFile().length())
                .body(resource);
    }

    /* ====================================================================================
     * 3. 私有辅助方法
     * ==================================================================================== */

    /**
     * 提取下载时展示给用户的文件名：
     * - 优先使用 resource 表中记录的原始文件（若将来有 Resource 关联）；
     * - 当前版本：从相对路径末尾提取 UUID 文件名 + 扩展名，保证下载文件可被打开；
     *   （resource 发布流程完成后，可改为由 Resource 表的 file_name 回填，体验更友好）
     */
    private String extractDownloadFileName(FileStorage storage, Path absPath) {
        if (storage == null || storage.getFilePath() == null) return "download";
        int slash = storage.getFilePath().lastIndexOf('/');
        return slash < 0 ? storage.getFilePath() : storage.getFilePath().substring(slash + 1);
    }

    /**
     * 根据扩展名猜测下载 Content-Type（默认 application/octet-stream 触发浏览器下载）
     */
    private String guessMediaType(String fileName) {
        String ext = FileUtil.getExtension(fileName);
        switch (ext) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "txt":
                return "text/plain; charset=utf-8";
            default:
                return "application/octet-stream";
        }
    }
}
