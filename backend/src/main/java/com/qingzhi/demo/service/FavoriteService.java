package com.qingzhi.demo.service;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.entity.Resource;

import java.util.Map;

/**
 * 收藏服务接口
 * <p>对应 PRD 2.2.3 普通用户自用 - 我的收藏。</p>
 */
public interface FavoriteService {

    /**
     * 收藏资源
     * <p>幂等：已收藏时不重复插入，返回收藏状态。
     * <p>可见性校验：仅允许收藏自己可见的资源（已通过的所有资源，或自己的未通过资源）。
     *
     * @param userId     当前用户ID
     * @param resourceId 要收藏的资源ID
     * @param userRole   当前用户角色（管理员/普通用户）
     * @return Map：{ favorited: boolean（本次是否发生新增收藏）, existed: boolean（调用前是否已收藏） }
     */
    Map<String, Object> favorite(Long userId, Long resourceId, String userRole);

    /**
     * 取消收藏
     * <p>幂等：未收藏时返回 false，不报异常。
     *
     * @return true=取消成功（原先是已收藏）；false=本就未收藏
     */
    boolean unfavorite(Long userId, Long resourceId);

    /**
     * 判断某资源是否被某用户收藏
     *
     * @return true=已收藏
     */
    boolean isFavorited(Long userId, Long resourceId);

    /**
     * 我的收藏列表（分页，按收藏时间倒序）
     * <p>只返回「已通过审核」的资源（未通过的资源即使被收藏也不应显示）。
     *
     * @param userId   当前用户ID
     * @param keyword  关键词（可选，模糊 title/description）
     * @param course   课程（可选，精确）
     * @param pageNum  页码，1起
     * @param pageSize 每页条数
     * @return 分页结果（records 是 Resource 列表）
     */
    PageResult<Resource> listMyFavorites(Long userId,
                                         String keyword, String course,
                                         Integer pageNum, Integer pageSize);
}
