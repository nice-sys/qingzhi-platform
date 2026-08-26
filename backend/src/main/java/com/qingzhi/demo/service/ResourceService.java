package com.qingzhi.demo.service;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.entity.Resource;

import java.time.LocalDate;

/**
 * 资源服务接口（普通用户侧）
 * <p>对应 PRD 2.3 资源管理模块：发布 / 修改 / 删除 / 已通过资源列表 / 资源详情 / 我的资源。
 * <p>管理员侧的审核/全局删除接口在 AdminService 中实现。</p>
 */
public interface ResourceService {

    /* ====================================================================================
     * 一、写操作（需登录 + 校验）
     * ==================================================================================== */

    /**
     * 发布资源（PRD 2.3.2 流程 1：用户提交 → 待审核）
     *
     * @param resourceDto 前端提交的资源信息（至少包含 title/course/fileStorageId；
     *                    可选 description/fileName/filePath/fileSize/fileExt/fileHash）
     * @param uploaderId  上传者用户ID（从 JWT 解析）
     * @return 新建的 resource.id
     */
    Long publishResource(Resource resourceDto, Long uploaderId);

    /**
     * 修改资源（PRD 2.3.2：已通过资源修改后自动回退为待审核）
     *
     * @param resourceDto 要修改的字段（必须有 id）
     * @param operatorId  当前操作人ID（必须是上传者本人才能修改；管理员在 AdminService 另有全局接口）
     * @return true=修改成功；false=无影响（异常情况直接抛 BusinessException）
     */
    boolean updateResource(Resource resourceDto, Long operatorId);

    /**
     * 删除自己的资源
     * <p>注意：删除成功后会同步释放关联的 fileStorage 引用（引用计数 -1，归零则删盘）。
     *
     * @param resourceId 资源ID
     * @param operatorId 当前操作人ID（必须是上传者本人；管理员全局删除走 AdminService）
     */
    void deleteMyResource(Long resourceId, Long operatorId);

    /* ====================================================================================
     * 二、读操作（列表 / 详情）
     * ==================================================================================== */

    /**
     * 已通过资源列表（PRD 2.3.4：所有用户可见）
     * <p>按 create_time DESC（最新发布在前）。
     *
     * @param keyword   关键词：模糊匹配 title / description（可选）
     * @param course    所属课程精确筛选（可选）
     * @param startDate create_time 起始日期（可选，含当天）
     * @param endDate   create_time 截止日期（可选，含当天）
     * @param pageNum   页码，从 1 开始（默认 1）
     * @param pageSize  每页条数（默认 10，最大 100）
     * @return 分页结果
     */
    PageResult<Resource> listApprovedResources(String keyword, String course,
                                               LocalDate startDate, LocalDate endDate,
                                               Integer pageNum, Integer pageSize);

    /**
     * 资源详情
     * <p>可见性规则：
     * <ul>
     *   <li>审核通过（1） → 所有登录用户可见</li>
     *   <li>待审核（0）或 已拒绝（2）→ 仅上传者本人或管理员可见</li>
     * </ul>
     *
     * @param resourceId 资源ID
     * @param viewerId   当前查看人ID（未登录传 null；传 null 则拒绝访问非已通过资源）
     * @param viewerRole 当前查看人角色（用于管理员可见判断；未登录传 null）
     * @return Resource 实体；未找到返回 null（Service 层已做可见性判断）
     */
    Resource getResourceDetail(Long resourceId, Long viewerId, String viewerRole);

    /**
     * 我的资源列表（PRD 2.2.3 普通用户自用）
     *
     * @param uploaderId   上传者用户ID（JWT 当前登录人）
     * @param keyword      关键词（可选）
     * @param course       课程（可选）
     * @param reviewStatus 审核状态过滤（可选；0=待审核 1=已通过 2=已拒绝）
     * @param startDate    创建时间起（可选）
     * @param endDate      创建时间止（可选）
     * @param pageNum      页码（默认 1）
     * @param pageSize     每页条数（默认 10，最大 100）
     * @return 分页结果，按 create_time DESC
     */
    PageResult<Resource> listMyResources(Long uploaderId,
                                         String keyword, String course, Integer reviewStatus,
                                         LocalDate startDate, LocalDate endDate,
                                         Integer pageNum, Integer pageSize);
}
