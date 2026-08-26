package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.entity.Favorite;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.ReviewStatusEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.FavoriteMapper;
import com.qingzhi.demo.mapper.ResourceMapper;
import com.qingzhi.demo.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务实现
 * <p>对应 PRD 2.2.3 普通用户自用 - 我的收藏。</p>
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private final FavoriteMapper favoriteMapper;
    private final ResourceMapper resourceMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, ResourceMapper resourceMapper) {
        this.favoriteMapper = favoriteMapper;
        this.resourceMapper = resourceMapper;
    }

    /* ====================================================================================
     * 1. 收藏资源
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> favorite(Long userId, Long resourceId, Integer userRole) {
        // 1. 基础参数校验
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);
        BusinessException.throwIfNull(resourceId, ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源ID不能为空");

        // 2. 资源存在性 + 可见性校验
        Resource res = resourceMapper.selectById(resourceId);
        BusinessException.throwIfNull(res, ResponseCodeEnum.RESOURCE_NOT_FOUND);
        assertResourceVisibleToUser(res, userId, userRole);

        // 3. 幂等判断：是否已收藏
        int already = favoriteMapper.countByUserAndResource(userId, resourceId);
        boolean existed = already > 0;
        boolean favorited = false;
        if (!existed) {
            // 4. 未收藏 → 插入（填充 create_time，避免 NOT NULL 报错）
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setResourceId(resourceId);
            fav.setCreateTime(java.time.LocalDateTime.now());
            int rows = favoriteMapper.insert(fav);
            favorited = rows > 0;
            if (favorited) {
                log.info("收藏成功：userId={}, resourceId={}, favoriteId={}",
                        userId, resourceId, fav.getId());
            }
        } else {
            log.info("重复收藏，跳过插入：userId={}, resourceId={}", userId, resourceId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorited", favorited); // 本次是否执行了新增
        result.put("existed", existed);     // 调用前是否已收藏
        return result;
    }

    /* ====================================================================================
     * 2. 取消收藏
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfavorite(Long userId, Long resourceId) {
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);
        BusinessException.throwIfNull(resourceId,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源ID不能为空");

        int rows = favoriteMapper.deleteByUserAndResource(userId, resourceId);
        if (rows > 0) {
            log.info("取消收藏成功：userId={}, resourceId={}", userId, resourceId);
            return true;
        }
        log.info("取消收藏：本就未收藏，userId={}, resourceId={}", userId, resourceId);
        return false;
    }

    /* ====================================================================================
     * 3. 是否已收藏
     * ==================================================================================== */

    @Override
    public boolean isFavorited(Long userId, Long resourceId) {
        if (userId == null || resourceId == null) return false;
        return favoriteMapper.countByUserAndResource(userId, resourceId) > 0;
    }

    /* ====================================================================================
     * 4. 我的收藏列表（分页）
     * ==================================================================================== */

    @Override
    public PageResult<Resource> listMyFavorites(Long userId,
                                                String keyword, String course,
                                                Integer pageNum, Integer pageSize) {
        BusinessException.throwIfNull(userId, ResponseCodeEnum.UNAUTHORIZED);

        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);
        keyword = normalizeKeyword(keyword);
        course = normalizeCourse(course);

        long total = favoriteMapper.countMyFavorites(userId, keyword, course);
        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        int offset = (pageNum - 1) * pageSize;
        List<Resource> records = favoriteMapper.selectMyFavoritesPage(
                userId, keyword, course, offset, pageSize);
        return PageResult.of(records == null ? Collections.emptyList() : records,
                pageNum, pageSize, total);
    }

    /* ====================================================================================
     * 私有辅助
     * ==================================================================================== */

    /**
     * 校验资源对当前用户是否可见（与 ResourceService.getResourceDetail 规则一致）
     * <ul>
     *   <li>已通过(1) → 所有登录用户可见</li>
     *   <li>待审核(0) / 已拒绝(2) → 仅上传者本人 或 管理员可见</li>
     * </ul>
     */
    private void assertResourceVisibleToUser(Resource res, Long viewerId, Integer viewerRole) {
        Integer status = res.getReviewStatus();
        if (ReviewStatusEnum.APPROVED.getCode() == (status == null ? -1 : status)) {
            return; // 已通过 → 可见
        }
        // 未通过
        boolean isOwner = viewerId != null && viewerId.equals(res.getUploaderId());
        boolean isAdmin = viewerRole != null && RoleEnum.ADMIN.getCode() == viewerRole;
        if (!isOwner && !isAdmin) {
            BusinessException.throwOf(ResponseCodeEnum.PERMISSION_DENIED,
                    "资源尚未通过审核，无法收藏");
        }
    }

    private static Integer normalizePageNum(Integer pageNum) {
        return (pageNum == null || pageNum < 1) ? Constants.DEFAULT_PAGE_NUM : pageNum;
    }

    private static Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) return Constants.DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, Constants.MAX_PAGE_SIZE);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }

    private static String normalizeCourse(String course) {
        if (course == null) return null;
        String c = course.trim();
        return c.isEmpty() ? null : c;
    }
}
