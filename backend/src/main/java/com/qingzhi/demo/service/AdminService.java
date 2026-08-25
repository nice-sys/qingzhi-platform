package com.qingzhi.demo.service;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.dto.request.AdminResetPasswordRequest;
import com.qingzhi.demo.dto.request.AdminUserCreateRequest;
import com.qingzhi.demo.dto.request.AdminUserUpdateRequest;
import com.qingzhi.demo.dto.request.ResourceQueryRequest;
import com.qingzhi.demo.dto.request.ResourceReviewRequest;
import com.qingzhi.demo.dto.request.UserQueryRequest;
import com.qingzhi.demo.dto.response.ResourceInfoResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;

/**
 * 管理员业务接口
 * <p>对应 PRD 2.2.1 角色权限矩阵（管理员专属 Y 项）
 * & 2.2.2 管理员权限详情：用户管理、资源管理、数据导入管理</p>
 */
public interface AdminService {

    /* ====================================================================================
     * 子模块 1：用户管理（PRD 2.2.2：增删改查 + 重置密码）
     * 对应权限矩阵：
     *  - 用户信息增删改查（所有用户） Y
     *  - 重置用户密码             Y
     * ==================================================================================== */

    /**
     * 分页查询用户列表（管理员）
     * <p>支持关键字（账号/姓名模糊）、角色、状态、院系等多条件查询</p>
     *
     * @param adminRole     当前操作者的角色（必须为 ADMIN，否则抛 1002 无权限）
     * @param queryRequest  查询条件 + 分页参数
     * @return 分页结果（用户信息列表 + 总数）
     */
    PageResult<UserInfoResponse> pageUsers(Integer adminRole, UserQueryRequest queryRequest);

    /**
     * 查询指定用户详情（管理员查看任意用户资料）
     *
     * @param adminRole 当前操作者的角色
     * @param userId    被查看的用户ID
     * @return 脱敏后的用户信息
     */
    UserInfoResponse getUserDetail(Integer adminRole, Long userId);

    /**
     * 管理员重置任意用户的密码（无需旧密码）
     * <p>对应 PRD 权限矩阵：重置用户密码 Y</p>
     * <ol>
     *   <li>校验当前操作者必须是管理员</li>
     *   <li>校验被操作的用户必须存在</li>
     *   <li>校验两次新密码一致 + 新密码格式合法</li>
     *   <li>新密码 MD5 加密后更新，同时解锁用户（若之前被锁则自动解除）</li>
     * </ol>
     *
     * @param adminRole 当前操作者的角色
     * @param request   重置密码请求（userId + newPassword + confirmNewPassword）
     */
    void resetUserPassword(Integer adminRole, AdminResetPasswordRequest request);

    /**
     * 管理员解锁用户（如手动解除账号锁定，无需等 15 分钟自动解除）
     *
     * @param adminRole 当前操作者的角色
     * @param userId    被解锁的用户ID
     */
    void unlockUser(Integer adminRole, Long userId);

    /**
     * 管理员删除用户（注意：保留本人账户不允许删除）
     *
     * @param adminRole    当前操作者的角色
     * @param adminUserId  当前操作者的用户ID（用于防止自己删自己）
     * @param userId       被删除的用户ID
     */
    void deleteUser(Integer adminRole, Long adminUserId, Long userId);

    /**
     * 管理员手动新增用户（PRD 2.2.2 用户管理：增）
     * <p>与"用户自行注册"不同：
     * <ul>
     *   <li>允许管理员直接创建任意角色（包括管理员自己）；</li>
     *   <li>允许指定初始账号、初始密码、院系等所有字段。</li>
     * </ul></p>
     *
     * @param adminRole 当前操作者角色
     * @param request   新增用户请求（必填：username/password/name/role）
     * @return 新增后的用户ID（useGeneratedKeys 回填）
     */
    Long createUser(Integer adminRole, com.qingzhi.demo.dto.request.AdminUserCreateRequest request);

    /**
     * 管理员编辑任意用户信息（PRD 2.2.2 用户管理：改）
     * <p>只更新请求中传入的非空字段，其余字段保持不变；
     * 同时提供"角色修改安全保护"：禁止把最后一个管理员改作普通用户。</p>
     *
     * @param adminRole    当前操作者角色
     * @param userId       被编辑用户ID
     * @param request      编辑请求（所有字段可空，null=不修改）
     */
    void updateUser(Integer adminRole, Long userId, com.qingzhi.demo.dto.request.AdminUserUpdateRequest request);

    /* ====================================================================================
     * 子模块 2：资源管理（PRD 2.2.2）
     * 对应权限矩阵：
     *  - 资源审核（通过/拒绝）       Y
     *  - 资源增删改查（所有资源）     Y
     * ==================================================================================== */

    /**
     * 管理员分页查询所有资源（全局资源列表）
     * <p>支持关键字（标题/描述/课程）/所属课程/审核状态/上传者/发布时间起止 多条件筛选；
     * 按发布时间倒序分页。</p>
     *
     * @param adminRole  当前操作者角色（必须管理员）
     * @param query      查询条件
     * @return 分页结果（ResourceInfoResponse）
     */
    PageResult<ResourceInfoResponse> pageResources(Integer adminRole, ResourceQueryRequest query);

    /**
     * 管理员审核资源（通过 / 拒绝）
     * <p>PRD 2.2.2 资源管理：对发布的资源进行审核（通过后才能公开发布）
     * & PRD 2.3.2 资源状态流转。</p>
     *
     * @param adminRole    当前操作者角色（必须管理员）
     * @param adminUserId  当前操作者用户ID（用于记录 review_admin_id）
     * @param request      审核请求（resourceId + approve + 拒绝理由）
     */
    void reviewResource(Integer adminRole, Long adminUserId, ResourceReviewRequest request);

    /**
     * 管理员删除任意资源（PRD 2.2.2 资源管理：全局增删改查）
     *
     * @param adminRole  当前操作者角色（必须管理员）
     * @param resourceId 被删除资源ID
     */
    void deleteResource(Integer adminRole, Long resourceId);

    /**
     * 管理员查看任意资源详情
     *
     * @param adminRole  当前操作者角色（必须管理员）
     * @param resourceId 资源ID
     */
    ResourceInfoResponse getResourceDetail(Integer adminRole, Long resourceId);

    /* ====================================================================================
     * 子模块 3：数据导入管理（PRD 2.2.2 + PRD 2.4）
     *  权限矩阵：Excel 批量导入师生信息 管理员 Y / 教师 - / 学生 -
     * ==================================================================================== */

    /**
     * Excel 批量导入师生信息
     * <p>对应 PRD 2.2.2「数据导入管理」 + 2.4.2 4 项合法性校验。</p>
     * <ol>
     *   <li>权限：必须管理员</li>
     *   <li>校验 1：必填字段完整性（学号/工号、姓名、院系、学生必填专业、初始密码）</li>
     *   <li>校验 2：学号/工号格式（长度上限 + 非空；可按规则扩展，此处使用 username 规则）</li>
     *   <li>校验 3：重复检查 —— Excel 内部不重复 & 与数据库 user 表 username 不重复</li>
     *   <li>校验 4：初始密码合法性（>=8位 且含数字+字母）</li>
     *   <li>手机号/邮箱：若填了，必须格式合法</li>
     *   <li>通过校验的行：加密密码后 insert user（批量或逐行）</li>
     * </ol>
     *
     * @param adminRole  当前操作者角色（必须管理员）
     * @param targetRole 导入目标角色（TEACHER 或 STUDENT；ADMIN 禁止导入）
     * @param file       上传的 Excel 文件（.xls 或 .xlsx）
     * @return 导入结果：total / successCount / failCount + 每行失败原因
     */
    com.qingzhi.demo.dto.response.ImportResultResponse importUsers(Integer adminRole,
                                                                    com.qingzhi.demo.enums.RoleEnum targetRole,
                                                                    org.springframework.web.multipart.MultipartFile file);
}
