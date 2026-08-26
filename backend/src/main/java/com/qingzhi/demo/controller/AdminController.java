package com.qingzhi.demo.controller;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.dto.request.AdminResetPasswordRequest;
import com.qingzhi.demo.dto.request.AdminUserCreateRequest;
import com.qingzhi.demo.dto.request.AdminUserUpdateRequest;
import com.qingzhi.demo.dto.request.ResourceQueryRequest;
import com.qingzhi.demo.dto.request.ResourceReviewRequest;
import com.qingzhi.demo.dto.request.UserQueryRequest;
import com.qingzhi.demo.dto.response.ResourceInfoResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.interceptor.JwtInterceptor;
import com.qingzhi.demo.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 * <p>接口前缀：<code>/api/admin</code>，对应 PRD 2.2.1 角色权限矩阵（管理员专属 Y 功能）
 * & PRD 2.2.2 管理员权限详情。</p>
 * <p>所有接口均需 JWT 鉴权，且当前用户 role 必须为 <b>ADMIN(0)</b>；
 * 非管理员访问任何 /api/admin/** 接口都会在 Service 层被 PermissionUtil 抛出
 * {@link com.qingzhi.demo.enums.ResponseCodeEnum#NO_PERMISSION} 无权限异常。</p>
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /* ====================================================================================
     * 子模块 1：用户管理（PRD 2.2.2 —— 管理员增删改查 + 重置密码 + 解锁）
     * ==================================================================================== */

    /**
     * 管理员分页查询所有用户
     * <p>PRD 2.2.1 权限矩阵：用户信息增删改查（所有用户） Y / 教师 - / 学生 -</p>
     * <p>支持 GET 方式：/api/admin/users?pageNum=1&pageSize=10&keyword=张&role=2&status=0</p>
     *
     * @param request  HTTP 请求（用于从 JWT 中取当前操作者的用户 ID 和角色）
     * @param query    查询条件（pageNum / pageSize / keyword / role / status / department）
     * @return 分页结果 records（List of UserInfoResponse 无敏感字段）
     */
    @GetMapping("/users")
    public Result<PageResult<UserInfoResponse>> pageUsers(HttpServletRequest request,
                                                          UserQueryRequest query) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        PageResult<UserInfoResponse> page = adminService.pageUsers(adminRole, query);
        return Result.success(page);
    }

    /**
     * 管理员查看任意用户详情
     * <p>PRD 2.2.2 用户管理：增删改查</p>
     *
     * @param request HTTP 请求
     * @param userId  被查看的用户ID（路径变量）
     */
    @GetMapping("/users/{userId}")
    public Result<UserInfoResponse> getUserDetail(HttpServletRequest request,
                                                  @PathVariable
                                                  @NotNull(message = "用户ID不能为空")
                                                  @Positive(message = "用户ID必须大于0")
                                                  Long userId) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        UserInfoResponse user = adminService.getUserDetail(adminRole, userId);
        return Result.success(user);
    }

    /**
     * 管理员重置任意用户的密码（无需提供旧密码）
     * <p>PRD 2.2.1 权限矩阵：重置用户密码 管理员 Y / 教师 - / 学生 -</p>
     * <p>重置密码后账户自动解锁（重置登录失败次数和锁定状态）。</p>
     *
     * @param request HTTP 请求（JWT 取当前管理员角色）
     * @param body    请求体：userId + newPassword + confirmNewPassword
     */
    @PostMapping("/users/reset-password")
    public Result<Void> resetUserPassword(HttpServletRequest request,
                                          @Valid @RequestBody AdminResetPasswordRequest body) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        adminService.resetUserPassword(adminRole, body);
        return Result.success("重置密码成功", null);
    }

    /**
     * 管理员手动解锁用户
     * <p>用户由于登录防暴力破解锁定 15 分钟后，管理员可通过该接口立即解锁，
     * 无需等待自动解锁。</p>
     *
     * @param request HTTP 请求
     * @param userId  被解锁用户ID（路径变量）
     */
    @PostMapping("/users/{userId}/unlock")
    public Result<Void> unlockUser(HttpServletRequest request,
                                   @PathVariable
                                   @NotNull(message = "用户ID不能为空")
                                   @Positive(message = "用户ID必须大于0")
                                   Long userId) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        adminService.unlockUser(adminRole, userId);
        return Result.success("解锁成功", null);
    }

    /**
     * 管理员删除用户
     * <p>PRD 2.2.1 权限矩阵：用户信息增删改查 Y</p>
     * <p>安全保护：
     * <ul>
     *   <li>禁止删除自己的账号；</li>
     *   <li>禁止删除系统中最后一个管理员账号。</li>
     * </ul></p>
     *
     * @param request HTTP 请求
     * @param userId  被删除的用户ID（路径变量）
     */
    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(HttpServletRequest request,
                                   @PathVariable
                                   @NotNull(message = "用户ID不能为空")
                                   @Positive(message = "用户ID必须大于0")
                                   Long userId) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        Long adminUserId = JwtInterceptor.getCurrentUserId(request);
        adminService.deleteUser(adminRole, adminUserId, userId);
        return Result.success("删除用户成功", null);
    }

    /**
     * 管理员新增用户（PRD 2.2.2 用户管理：增）
     * <p>与「用户自行注册」区别：管理员可直接创建任意角色账户，指定初始密码/院系等全部字段。</p>
     *
     * @param request HTTP 请求（取当前管理员角色）
     * @param body    请求体（username/password/name/role 必填）
     * @return data = 新增的用户ID
     */
    @PostMapping("/users")
    public Result<Long> createUser(HttpServletRequest request,
                                   @Valid @RequestBody AdminUserCreateRequest body) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        Long newUserId = adminService.createUser(adminRole, body);
        return Result.success("新增用户成功", newUserId);
    }

    /**
     * 管理员编辑任意用户信息（PRD 2.2.2 用户管理：改）
     * <p>所有字段可选：传什么改什么，不传的字段保持原值不变。</p>
     * <p>安全保护：若目标是最后一个管理员，则不允许将角色改为非管理员。</p>
     *
     * @param request HTTP 请求
     * @param userId  被编辑的用户ID（路径变量）
     * @param body    请求体（所有字段可空）
     */
    @PutMapping("/users/{userId}")
    public Result<Void> updateUser(HttpServletRequest request,
                                   @PathVariable
                                   @NotNull(message = "用户ID不能为空")
                                   @Positive(message = "用户ID必须大于0")
                                   Long userId,
                                   @Valid @RequestBody AdminUserUpdateRequest body) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        adminService.updateUser(adminRole, userId, body);
        return Result.success("编辑用户成功", null);
    }

    /* ====================================================================================
     * 子模块 2：资源管理（PRD 2.2.2 —— 全局审核 + 增删改查）
     * ==================================================================================== */

    /**
     * 管理员分页查询所有资源（全局资源列表）
     * <p>PRD 2.2.2 资源管理：对资源进行增删改查；2.3.2 支持按发布时间范围查询，结果按时间倒序。</p>
     */
    @GetMapping("/resources")
    public Result<PageResult<ResourceInfoResponse>> pageResources(HttpServletRequest request,
                                                                  ResourceQueryRequest query) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        PageResult<ResourceInfoResponse> page = adminService.pageResources(adminRole, query);
        return Result.success(page);
    }

    /**
     * 管理员查看任意资源详情
     */
    @GetMapping("/resources/{resourceId}")
    public Result<ResourceInfoResponse> getResourceDetail(HttpServletRequest request,
                                                          @PathVariable
                                                          @NotNull(message = "资源ID不能为空")
                                                          @Positive(message = "资源ID必须大于0")
                                                          Long resourceId) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        ResourceInfoResponse r = adminService.getResourceDetail(adminRole, resourceId);
        return Result.success(r);
    }

    /**
     * 管理员审核资源（通过 / 拒绝）
     * <p>PRD 2.2.2 资源管理：审核（通过后才能公开发布）；PRD 2.3.2 状态流转。</p>
     * <ul>
     *   <li>通过后：所有用户可见</li>
     *   <li>拒绝：必须填写拒绝理由，用户可修改后重新提交</li>
     * </ul>
     */
    @PostMapping("/resources/review")
    public Result<Void> reviewResource(HttpServletRequest request,
                                       @Valid @RequestBody ResourceReviewRequest body) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        Long adminUserId = JwtInterceptor.getCurrentUserId(request);
        adminService.reviewResource(adminRole, adminUserId, body);
        String msg = Boolean.TRUE.equals(body.getApprove()) ? "审核通过成功" : "审核拒绝成功";
        return Result.success(msg, null);
    }

    /**
     * 管理员删除任意资源（全局删除）
     */
    @DeleteMapping("/resources/{resourceId}")
    public Result<Void> deleteResource(HttpServletRequest request,
                                       @PathVariable
                                       @NotNull(message = "资源ID不能为空")
                                       @Positive(message = "资源ID必须大于0")
                                       Long resourceId) {
        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        adminService.deleteResource(adminRole, resourceId);
        return Result.success("删除资源成功", null);
    }

    /* ====================================================================================
     * 子模块 3：数据导入管理（PRD 2.2.2 + PRD 2.4 Excel 批量导入）
     * ==================================================================================== */

    /**
     * Excel 批量导入师生信息
     * <p>PRD 2.2.2 管理员权限：上传符合规范的 Excel 批量导入师生信息；
     * 2.4.2 导入合法性校验（学号/工号、重复、必填字段、初始密码）全部通过。</p>
     *
     * <p>请求方式：multipart/form-data</p>
     * <pre>
     * 参数：
     *   role  — 必填，1=导入教师，2=导入学生（0=管理员不允许批量导入）
     *   file  — 必填，.xls 或 .xlsx 格式的 Excel 文件
     * </pre>
     *
     * <p>返回结构：total（总行数）/ successCount（成功数）/ failCount（失败数）
     *              + failList（每行失败行号+账号+原因，便于用户修正Excel）</p>
     *
     * <p>加分项「导入回滚」：若需启用"任何一行失败则全部不入库"的全量回滚模式，
     * 只需在 importUsers 返回前加一行
     * {@code if (result.hasError()) throw new BusinessException(...)}，
     * 因为 Service 已加 @Transactional 会自动回滚。</p>
     */
    @PostMapping("/users/import")
    public Result<com.qingzhi.demo.dto.response.ImportResultResponse> importUsers(
            HttpServletRequest request,
            @RequestParam("role")
            @NotNull(message = "导入角色不能为空")
            @Min(value = 1, message = "角色编码非法（1=教师，2=学生）")
            @Max(value = 2, message = "角色编码非法（1=教师，2=学生）")
            Integer role,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        Integer adminRole = JwtInterceptor.getCurrentUserRole(request);
        com.qingzhi.demo.enums.RoleEnum target = com.qingzhi.demo.enums.RoleEnum.of(role);
        com.qingzhi.demo.dto.response.ImportResultResponse result =
                adminService.importUsers(adminRole, target, file);
        return Result.success("导入完成", result);
    }
}
