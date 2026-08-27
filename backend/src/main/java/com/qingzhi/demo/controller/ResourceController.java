package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.service.ResourceService;
import com.qingzhi.demo.utils.FileUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源模块控制器（普通用户侧）
 * <p>对应 PRD 2.3 资源管理。
 * <p>接口前缀 /api/resource，已在 WebConfig 注册 JwtInterceptor。
 * <p>所有接口当前用户信息统一通过 JwtInterceptor.getCurrentUserId() / getCurrentUserRole() 静态方法获取，
 * 避免常量名拼写错误。
 */
@RestController
@RequestMapping("/resource")
public class ResourceController {

    private final ResourceService resourceService;
    private final FileService fileService;

    public ResourceController(ResourceService resourceService, FileService fileService) {
        this.resourceService = resourceService;
        this.fileService = fileService;
    }

    /* ====================================================================================
     * 1. 发布资源
     *    POST /api/resource/publish
     *    Body（JSON）：title / course / description(可选) / fileName / filePath / fileSize(可选) / fileExt(可选) / fileHash(可选)
     *    返回：Result<Map<String, Object>>  { resourceId: Long }
     * ==================================================================================== */

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestBody Resource body,
                                               HttpServletRequest request) {
        Long uploaderId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);

        Long resourceId = resourceService.publishResource(body, uploaderId);

        Map<String, Object> data = new HashMap<>();
        data.put("resourceId", resourceId);
        return Result.success(data);
    }

    /* ====================================================================================
     * 2. 修改资源
     *    POST /api/resource/update
     *    Body（JSON）：id（必填，resourceId）+ 要修改的字段（title/course/description/fileName/filePath/fileSize/fileExt/fileHash）
     *    返回：Result<Map<String, Object>>  { updated: boolean }
     * ==================================================================================== */

    @PostMapping("/update")
    public Result<Map<String, Object>> update(@RequestBody Resource body,
                                              HttpServletRequest request) {
        Long operatorId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);

        boolean updated = resourceService.updateResource(body, operatorId);

        Map<String, Object> data = new HashMap<>();
        data.put("updated", updated);
        return Result.success(data);
    }

    /* ====================================================================================
     * 3. 删除我的资源
     *    POST /api/resource/delete
     *    Body（JSON）：{ id: Long }
     *    返回：Result<Map<String, Object>>  { deleted: boolean }
     * ==================================================================================== */

    @PostMapping("/delete")
    public Result<Map<String, Object>> delete(@RequestBody Map<String, Long> body,
                                              HttpServletRequest request) {
        Long operatorId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);
        Long resourceId = body != null ? body.get("id") : null;
        BusinessException.throwIfNull(resourceId, ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源ID不能为空");

        resourceService.deleteMyResource(resourceId, operatorId);

        Map<String, Object> data = new HashMap<>();
        data.put("deleted", true);
        return Result.success(data);
    }

    /* ====================================================================================
     * 4. 已通过资源列表（公开列表；所有已登录用户可见）
     *    GET /api/resource/list
     *    Query：keyword(可选) / course(可选) / startDate(可选, yyyy-MM-dd) / endDate(可选)
     *           / pageNum(默认1) / pageSize(默认10)
     *    返回：Result<PageResult<Resource>>
     * ==================================================================================== */

    @GetMapping("/list")
    public Result<PageResult<Resource>> listApproved(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {

        PageResult<Resource> page = resourceService.listApprovedResources(
                keyword, course, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    /* ====================================================================================
     * 5. 资源详情
     *    GET /api/resource/{id}
     *    PathVariable: id = resource.id
     *    可见性：已通过 → 任何登录用户；待审核/已拒绝 → 仅上传者本人或管理员
     *    返回：Result<Resource>
     * ==================================================================================== */

    @GetMapping("/{id}")
    public Result<Resource> detail(@PathVariable("id") Long id,
                                   HttpServletRequest request) {
        Long viewerId = JwtInterceptor.getCurrentUserId(request);
        Integer viewerRole = JwtInterceptor.getCurrentUserRole(request);

        Resource resource = resourceService.getResourceDetail(id, viewerId, viewerRole);
        return Result.success(resource);
    }

    /* ====================================================================================
     * 6. 下载资源（PRD 2.3.4）
     *    GET /api/resource/{id}/download
     *    PathVariable: id = resource.id
     *    权限：与详情接口完全一致（仅自己可见资源或审核通过）
     *    副作用：download_count 原子自增 +1（事务保证）
     *    响应：Content-Type = 根据扩展名；Content-Disposition attachment; filename=原始文件名（支持中文）
     * ==================================================================================== */

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        Long viewerId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(viewerId, ResponseCodeEnum.UNAUTHORIZED);
        Integer viewerRole = JwtInterceptor.getCurrentUserRole(request);

        // 1. 可见性校验 + 下载计数自增（事务内原子自增）
        Resource res = resourceService.downloadResource(id, viewerId, viewerRole);

        // 2. 解析磁盘绝对路径并校验
        Path absPath = fileService.resolveFile(res.getFilePath());
        BusinessException.throwIfNull(absPath,
                ResponseCodeEnum.FILE_NOT_FOUND, "资源文件磁盘记录缺失，请联系管理员");

        java.io.File diskFile = absPath.toFile();
        BusinessException.throwIf(!diskFile.exists(),
                ResponseCodeEnum.FILE_NOT_FOUND, "资源文件在服务器上不存在，请联系管理员");

        // 3. Content-Disposition：使用 Resource.file_name（用户上传时的原始文件名，体验友好）
        String originalName = res.getFileName();
        if (originalName == null || originalName.isEmpty()) {
            // 兜底：从相对路径末尾提取 UUID 文件名
            String p = res.getFilePath();
            int slash = p == null ? -1 : p.lastIndexOf('/');
            originalName = (slash < 0) ? "download" : p.substring(slash + 1);
        }
        String encoded;
        try {
            encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "download";
        }
        String contentDisposition = "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;

        // 4. Content-Type：扩展名识别（默认 application/octet-stream 触发浏览器下载）
        String contentType = guessMediaType(originalName);

        // 5. 响应 body：FileSystemResource 流式传输；Spring 自动用 Transfer-Encoding: chunked，不会 OOM
        org.springframework.core.io.Resource body = new FileSystemResource(diskFile);
        long contentLength = res.getFileSize() != null ? res.getFileSize() : diskFile.length();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION)
                .contentLength(contentLength)
                .body(body);
    }

    /* ====================================================================================
     * 7. 我的资源列表
     *    GET /api/resource/my
     *    Query：keyword(可选) / course(可选) / reviewStatus(可选，0/1/2)
     *           / startDate(可选) / endDate(可选)
     *           / pageNum(默认1) / pageSize(默认10)
     *    返回：Result<PageResult<Resource>>
     * ==================================================================================== */

    @GetMapping("/my")
    public Result<PageResult<Resource>> myResources(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "reviewStatus", required = false) Integer reviewStatus,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {

        Long uploaderId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);

        PageResult<Resource> page = resourceService.listMyResources(
                uploaderId, keyword, course, reviewStatus, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    /* ====================================================================================
     * 辅助：根据扩展名猜测下载 Content-Type（默认 application/octet-stream 触发浏览器下载）
     * ==================================================================================== */

    private static String guessMediaType(String fileName) {
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
