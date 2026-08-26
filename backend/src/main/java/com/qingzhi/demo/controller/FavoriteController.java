package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 收藏模块控制器
 * <p>对应 PRD 2.2.3 普通用户自用 - 我的收藏。
 * <p>接口前缀 /api/favorite，需登录（已在 WebConfig 注册 JwtInterceptor）。
 * <p>所有当前用户信息统一通过 JwtInterceptor.getXxx() 静态方法获取。</p>
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /* ====================================================================================
     * 1. 收藏资源
     *    POST /api/favorite/add
     *    Body（JSON）：{ resourceId: Long }
     *    返回：Result<Map<String, Object>> { favorited, existed }
     * ==================================================================================== */

    @PostMapping("/add")
    public Result<Map<String, Object>> add(@RequestBody Map<String, Long> body,
                                           HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);
        Long resourceId = body != null ? body.get("resourceId") : null;
        BusinessException.throwIfNull(resourceId,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "resourceId 不能为空");

        Integer userRole = JwtInterceptor.getCurrentUserRole(request);
        Map<String, Object> data = favoriteService.favorite(userId, resourceId, userRole);
        return Result.success(data);
    }

    /* ====================================================================================
     * 2. 取消收藏
     *    POST /api/favorite/remove
     *    Body（JSON）：{ resourceId: Long }
     *    返回：Result<Map<String, Object>> { removed: boolean }
     * ==================================================================================== */

    @PostMapping("/remove")
    public Result<Map<String, Object>> remove(@RequestBody Map<String, Long> body,
                                              HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);
        Long resourceId = body != null ? body.get("resourceId") : null;
        BusinessException.throwIfNull(resourceId,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "resourceId 不能为空");

        boolean removed = favoriteService.unfavorite(userId, resourceId);
        Map<String, Object> data = new HashMap<>();
        data.put("removed", removed);
        return Result.success(data);
    }

    /* ====================================================================================
     * 3. 判断某资源是否已收藏
     *    GET /api/favorite/check?resourceId=xxx
     *    返回：Result<Map<String, Object>> { favorited: boolean }
     * ==================================================================================== */

    @GetMapping("/check")
    public Result<Map<String, Object>> check(
            @RequestParam("resourceId") Long resourceId,
            HttpServletRequest request) {
        Long userId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);

        boolean favorited = favoriteService.isFavorited(userId, resourceId);
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        return Result.success(data);
    }

    /* ====================================================================================
     * 4. 我的收藏列表（分页）
     *    GET /api/favorite/my
     *    Query：keyword(可选) / course(可选)
     *           / pageNum(默认1) / pageSize(默认10)
     *    返回：Result<PageResult<Resource>>  （按收藏时间倒序，仅含已通过的资源）
     * ==================================================================================== */

    @GetMapping("/my")
    public Result<PageResult<Resource>> myFavorites(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {

        Long userId = JwtInterceptor.getCurrentUserId(request);
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);

        PageResult<Resource> page = favoriteService.listMyFavorites(
                userId, keyword, course, pageNum, pageSize);
        return Result.success(page);
    }
}
