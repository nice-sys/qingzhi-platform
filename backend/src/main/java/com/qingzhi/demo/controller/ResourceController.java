package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源模块控制器（普通用户侧）
 * <p>对应 PRD 2.3 资源管理。
 * <p>接口前缀 /api/resource，已在 WebConfig 注册 JwtInterceptor（除公开列表/详情外需登录）。
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
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
        Long uploaderId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
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
        Long operatorId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
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
        Long operatorId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
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

        Long viewerId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
        BusinessException.throwIfNull(viewerId, ResponseCodeEnum.UNAUTHORIZED);

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
        Long viewerId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
        BusinessException.throwIfNull(viewerId, ResponseCodeEnum.UNAUTHORIZED);
        String viewerRole = (String) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ROLE);

        Resource resource = resourceService.getResourceDetail(id, viewerId, viewerRole);
        return Result.success(resource);
    }

    /* ====================================================================================
     * 6. 我的资源列表
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

        Long uploaderId = (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID);
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);

        PageResult<Resource> page = resourceService.listMyResources(
                uploaderId, keyword, course, reviewStatus, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }
}
