package com.qingzhi.demo.controller;

import com.qingzhi.demo.annotation.RateLimit;
import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.entity.FileStorage;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.service.ResourceService;
import com.qingzhi.demo.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
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
@RequestMapping("/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    private final ResourceService resourceService;

    public FileController(FileService fileService, ResourceService resourceService) {
        this.fileService = fileService;
        this.resourceService = resourceService;
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
     * 2. 文件下载（按 Resource.id 下载，PRD 流程：在资源详情页点击下载按钮 → 传 resourceId）
     *    GET /api/file/download/{resourceId}
     *    PathVariable: resourceId = resource.id
     *    响应：application/octet-stream，浏览器触发下载
     *    副作用：reviewStatus=1（已通过）时，resource.download_count 原子 +1
     * ==================================================================================== */

    @GetMapping("/download/{resourceId}")
    @RateLimit(dimension = RateLimit.LimitDimension.USER,
            windowSeconds = 60,
            maxRequests = 5,
            skipForAdmin = true,
            errorCode = ResponseCodeEnum.OPERATION_TOO_FREQUENT)
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable("resourceId") Long resourceId,
                                             HttpServletRequest request) {
        // 1. 登录态校验（JwtInterceptor 已注入当前用户，但这里兜底校验，防止匿名访问下载接口）
        Long currentUserId = JwtInterceptor.getCurrentUserId(request);
        Integer currentUserRole = JwtInterceptor.getCurrentUserRole(request);
        BusinessException.throwIf(currentUserId == null,
                ResponseCodeEnum.UNAUTHORIZED);

        // 2. 用 resourceId 走 ResourceService.downloadResource：
        //    - 校验资源可见性（审核状态/上传者/管理员权限）
        //    - 原子 download_count +1
        //    - 返回 Resource entity（含 fileName / filePath / fileHash 等）
        com.qingzhi.demo.entity.Resource resource = resourceService.downloadResource(resourceId, currentUserId, currentUserRole);

        // 3. 根据 Resource.filePath 解析磁盘绝对路径并校验文件存在性
        BusinessException.throwIfBlank(resource.getFilePath(),
                ResponseCodeEnum.FILE_NOT_FOUND, "该资源尚未上传文件，无法下载");
        Path absPath = fileService.resolveFile(resource.getFilePath());
        BusinessException.throwIfNull(absPath,
                ResponseCodeEnum.FILE_NOT_FOUND, "文件磁盘记录缺失，请联系管理员");

        // 4. 组装下载响应（Content-Disposition 支持中文文件名）
        org.springframework.core.io.Resource fileResource = new FileSystemResource(absPath.toFile());
        String originalName = resource.getFileName();
        if (originalName == null || originalName.isEmpty()) {
            int slashIdx = (resource.getFilePath() == null) ? -1 : resource.getFilePath().lastIndexOf('/');
            originalName = (slashIdx < 0) ? resource.getFilePath() : resource.getFilePath().substring(slashIdx + 1);
            if (originalName == null || originalName.isEmpty()) originalName = "download";
        }

        String encoded;
        try {
            encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "download";
        }
        String contentDisposition = "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;

        String contentType = guessMediaType(originalName);
        Long fileLen = resource.getFileSize();
        if (fileLen == null || fileLen < 0) {
            fileLen = absPath.toFile().length();
        }

        log.info("资源下载完成：resourceId={}, viewerId={}, downloadCount={}",
                resourceId, currentUserId, resource.getDownloadCount());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION)
                .contentLength(fileLen)
                .body(fileResource);
    }

    /* ====================================================================================
     * 3. 私有辅助方法
     * ==================================================================================== */

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
