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
     * 插入新资源（发布资源时调用）
     *
     * @param resource 资源实体（需包含 title/course/uploaderId/fileName/filePath 等必填字段）
     * @return 影响行数；成功插入返回 1
     */
    int insert(Resource resource);

    /**
     * 根据主键动态更新资源信息（修改资源时调用）
     * <p>只更新传入非空的字段，避免覆盖原值。
     * <p>修改已通过的资源后，review_status 应由 Service 层显式回退为待审核（0）。
     *
     * @param resource 需包含 id + 要修改的字段（其余字段留 null 即可不更新）
     * @return 影响行数
     */
    int updateById(Resource resource);

    /**
     * 审核资源（通过 / 拒绝）
     * <p>更新 review_status / reject_reason / review_admin_id / review_time</p>
     */
    int updateReviewStatus(Resource resource);

    /**
     * 根据主键删除资源（管理员 / 用户删除自己的资源共用）
     */
    int deleteById(@Param("id") Long id);

    /**
     * 下载量 +1（原子自增）
     * <p>对应 PRD 4.2 字段 resource.download_count：每次成功下载后自增，避免并发下丢失更新。
     *
     * @param id 资源ID
     * @return 影响行数（资源不存在返回 0，成功返回 1）
     */
    int incrementDownloadCount(@Param("id") Long id);
}
