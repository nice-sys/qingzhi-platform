package com.qingzhi.demo.mapper;

import com.qingzhi.demo.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏 Mapper 接口（PRD 2.2.3 普通用户自用 - 我的收藏）
 * <p>对应 PRD 4.3 favorite 表：id / user_id / resource_id / create_time</p>
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 新增收藏记录
     *
     * @param favorite userId + resourceId 必填；createTime 可选（缺省 NOW()）
     * @return 影响行数；useGeneratedKeys 回填 id 到 favorite.id
     */
    int insert(Favorite favorite);

    /**
     * 按 userId + resourceId 删除（取消收藏）
     *
     * @return 影响行数；未收藏返回 0
     */
    int deleteByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    /**
     * 按主键删除（预留）
     */
    int deleteById(@Param("id") Long id);

    /**
     * 判断某用户是否已收藏某资源（返回 1 = 已收藏；0 = 未收藏）
     */
    int countByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    /**
     * 按 userId + resourceId 查询单条记录（用于幂等校验 / 查 id）
     */
    Favorite selectByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    /**
     * 某用户收藏的资源总数（分页用 total）
     */
    long countMyFavorites(@Param("userId") Long userId,
                          @Param("keyword") String keyword,
                          @Param("course") String course);

    /**
     * 某用户收藏的资源列表（分页，按收藏时间倒序 = favorite.create_time DESC）
     * <p>注意：返回的是 Resource 行（联表查），这样前端直接拿到资源详情不用二次请求。
     *
     * @return List<Resource>，实际字段与 Resource 相同（联表 favorite + resource）。
     */
    List<com.qingzhi.demo.entity.Resource> selectMyFavoritesPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("course") String course,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);
}
