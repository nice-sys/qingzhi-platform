package com.qingzhi.demo.mapper;

import com.qingzhi.demo.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 资源 Mapper 接口（PRD 2.3 资源管理）
 */
@Mapper
public interface ResourceMapper {

    /**
     * 按条件统计资源总数（用于分页 total）
     */
    long countResources(@Param("keyword") String keyword,
                        @Param("course") String course,
                        @Param("reviewStatus") Integer reviewStatus,
                        @Param("uploaderId") Long uploaderId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    /**
     * 按条件分页查询资源列表（按 create_time DESC，即发布时间倒序）
     */
    List<Resource> selectResourcesPage(@Param("keyword") String keyword,
                                       @Param("course") String course,
                                       @Param("reviewStatus") Integer reviewStatus,
                                       @Param("uploaderId") Long uploaderId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /**
     * 根据主键查询资源
     */
    Resource selectById(@Param("id") Long id);

    /**
     * 审核资源（通过 / 拒绝）
     * <p>更新 review_status / reject_reason / review_admin_id / review_time</p>
     */
    int updateReviewStatus(Resource resource);

    /**
     * 根据主键删除资源（管理员 / 用户删除自己的资源共用）
     */
    int deleteById(@Param("id") Long id);
}
