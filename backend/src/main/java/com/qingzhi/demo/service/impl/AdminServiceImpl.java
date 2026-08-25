package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.dto.request.AdminResetPasswordRequest;
import com.qingzhi.demo.dto.request.AdminUserCreateRequest;
import com.qingzhi.demo.dto.request.AdminUserUpdateRequest;
import com.qingzhi.demo.dto.request.ResourceQueryRequest;
import com.qingzhi.demo.dto.request.ResourceReviewRequest;
import com.qingzhi.demo.dto.request.UserQueryRequest;
import com.qingzhi.demo.dto.response.ExcelUserRow;
import com.qingzhi.demo.dto.response.ImportResultResponse;
import com.qingzhi.demo.dto.response.ResourceInfoResponse;
import com.qingzhi.demo.dto.response.UserInfoResponse;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.entity.User;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.ReviewStatusEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.ResourceMapper;
import com.qingzhi.demo.mapper.UserMapper;
import com.qingzhi.demo.service.AdminService;
import com.qingzhi.demo.utils.ExcelUtil;
import com.qingzhi.demo.utils.PasswordUtil;
import com.qingzhi.demo.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员业务实现
 * <p>所有方法入口统一先调用 PermissionUtil.assertAdmin(role) 校验权限，
 * 非管理员直接抛出 1002 无权限异常，对应 PRD 2.2.1 权限矩阵中的管理员专属 Y 项。</p>
 */
@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;

    @Autowired
    public AdminServiceImpl(UserMapper userMapper, ResourceMapper resourceMapper) {
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
    }

    /* ====================================================================================
     * 子模块 1：用户管理（PRD 2.2.2）
     * 权限矩阵：
     *  - 用户信息增删改查（所有用户） Y
     *  - 重置用户密码             Y
     * ==================================================================================== */

    @Override
    public PageResult<UserInfoResponse> pageUsers(Integer adminRole, UserQueryRequest queryRequest) {
        // 1. 权限校验：必须管理员
        PermissionUtil.assertAdmin(adminRole);

        // 2. 规范化分页参数（非法值兜底，防止负数等）
        if (queryRequest == null) {
            queryRequest = new UserQueryRequest();
        }
        queryRequest.normalize();

        // 3. 查询总数（用于计算 pages）
        long total = userMapper.countUsers(
                queryRequest.getKeyword(),
                queryRequest.getRole(),
                queryRequest.getStatus(),
                queryRequest.getDepartment()
        );

        // 4. total 为 0 直接返回空分页（避免多余 SQL）
        if (total == 0) {
            return PageResult.empty(queryRequest.getPageNum(), queryRequest.getPageSize());
        }

        // 5. 分页查询用户列表
        List<User> users = userMapper.selectUsersPage(
                queryRequest.getKeyword(),
                queryRequest.getRole(),
                queryRequest.getStatus(),
                queryRequest.getDepartment(),
                queryRequest.getOffset(),
                queryRequest.getPageSize()
        );

        // 6. 脱敏转换为 UserInfoResponse（从实体中剥离 password 等敏感字段）
        List<UserInfoResponse> records = users.stream()
                .map(UserInfoResponse::fromEntity)
                .collect(Collectors.toList());

        // 7. 返回带 total/pages/pageNum/pageSize/records 的统一分页结构
        return PageResult.of(records, queryRequest.getPageNum(), queryRequest.getPageSize(), total);
    }

    @Override
    public UserInfoResponse getUserDetail(Integer adminRole, Long userId) {
        // 1. 权限校验：必须管理员
        PermissionUtil.assertAdmin(adminRole);

        // 2. 用户 ID 必须存在
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "用户ID不能为空");

        // 3. 查询用户
        User user = userMapper.selectById(userId);
        BusinessException.throwIf(user == null, ResponseCodeEnum.USER_NOT_EXIST);

        // 4. 脱敏返回
        return UserInfoResponse.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Integer adminRole, AdminResetPasswordRequest request) {
        // 1. 权限校验：必须是管理员（PRD 权限矩阵：重置用户密码 Y）
        PermissionUtil.checkResetPasswordPermission(adminRole);

        // 2. 请求基本非空（在 @Valid 基础上兜底）
        BusinessException.throwIf(request == null, ResponseCodeEnum.PARAM_ERROR, "重置密码请求不能为空");
        BusinessException.throwIf(request.getUserId() == null || request.getUserId() <= 0,
                ResponseCodeEnum.PARAM_ERROR, "用户ID不能为空");

        // 3. 两次新密码必须一致（DTO 校验无法覆盖到的 Service 层兜底）
        BusinessException.throwIf(!request.isNewPasswordMatch(),
                ResponseCodeEnum.FAILURE, "两次新密码输入不一致");

        // 4. 新密码格式校验（与普通用户改密统一规则）
        PasswordUtil.validateFormat(request.getNewPassword());

        // 5. 确认被操作的用户存在
        User targetUser = userMapper.selectById(request.getUserId());
        BusinessException.throwIf(targetUser == null, ResponseCodeEnum.USER_NOT_EXIST);

        // 6. 新密码 MD5 加密后更新（同时重置锁定状态——密码重置后账户自动解锁）
        String encryptedPassword = PasswordUtil.encrypt(request.getNewPassword());
        int rows = userMapper.updatePassword(request.getUserId(), encryptedPassword);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "重置密码失败：未找到用户记录");

        // 7. 重置登录失败信息（将状态/锁定期限/失败次数全部清除，用户使用新密码即可登录）
        userMapper.resetLoginFailInfo(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Integer adminRole, Long userId) {
        // 1. 权限校验：必须管理员（解锁与重置密码同归用户管理权限）
        PermissionUtil.assertAdmin(adminRole);
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "用户ID不能为空");

        // 2. 校验被解锁用户必须存在
        User user = userMapper.selectById(userId);
        BusinessException.throwIf(user == null, ResponseCodeEnum.USER_NOT_EXIST);

        // 3. 重置失败次数/锁定状态/锁定时间
        int rows = userMapper.resetLoginFailInfo(userId);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "解锁用户失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer adminRole, Long adminUserId, Long userId) {
        // 1. 权限校验：必须管理员
        PermissionUtil.checkUserManagePermission(adminRole);
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "被删除用户ID不能为空");

        // 2. 安全保护：管理员禁止删除自己（至少保留一个管理员账号）
        BusinessException.throwIf(adminUserId != null && adminUserId.equals(userId),
                ResponseCodeEnum.FAILURE, "不能删除自己的账号");

        // 3. 校验被删除用户必须存在
        User user = userMapper.selectById(userId);
        BusinessException.throwIf(user == null, ResponseCodeEnum.USER_NOT_EXIST);

        // 4. 安全保护：禁止删除最后一个管理员（避免系统彻底锁死）
        if (user.isAdmin()) {
            long adminCount = countAdminUsers();
            if (adminCount <= 1) {
                BusinessException.throwOf(ResponseCodeEnum.FAILURE, "必须至少保留一个管理员账号");
            }
        }

        // 5. 执行删除
        int rows = userMapper.deleteById(userId);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "删除用户失败");
    }

    /* ====================================================================================
     * 私有工具方法
     * ==================================================================================== */

    /**
     * 统计当前系统中管理员数量（防止删除最后一个管理员后系统无法登录管理后台）
     */
    private long countAdminUsers() {
        // 复用 countUsers：role=0（ADMIN）表示管理员，其它条件全部 null
        return userMapper.countUsers(null, 0, null, null);
    }

    /* ====================================================================================
     * 管理员新增/编辑用户（PRD 2.2.2 用户管理：增 + 改）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(Integer adminRole, AdminUserCreateRequest request) {
        // 1. 权限校验：必须管理员
        PermissionUtil.checkUserManagePermission(adminRole);
        BusinessException.throwIf(request == null, ResponseCodeEnum.PARAM_ERROR, "新增用户请求不能为空");

        // 2. 账号必须全局唯一（复用 selectByUsername 查重）
        User exist = userMapper.selectByUsername(request.getUsername());
        BusinessException.throwIf(exist != null, ResponseCodeEnum.USERNAME_EXIST);

        // 3. 密码格式 + 加密
        PasswordUtil.validateFormat(request.getPassword());
        String encryptedPassword = PasswordUtil.encrypt(request.getPassword());

        // 4. 组装用户实体（状态未传则默认为 0）
        User user = new User();
        user.setUsername(trimToNull(request.getUsername()));
        user.setPassword(encryptedPassword);
        user.setName(trimToNull(request.getName()));
        user.setRole(request.getRole());
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setDepartment(trimToNull(request.getDepartment()));
        user.setMajor(trimToNull(request.getMajor()));
        user.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        user.setLoginFailCount(0);

        // 5. 执行插入（useGeneratedKeys 回填 id）
        int rows = userMapper.insert(user);
        BusinessException.throwIf(rows != 1 || user.getId() == null,
                ResponseCodeEnum.FAILURE, "新增用户失败");

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Integer adminRole, Long userId, AdminUserUpdateRequest request) {
        // 1. 权限校验：必须管理员
        PermissionUtil.checkUserManagePermission(adminRole);
        BusinessException.throwIf(userId == null || userId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "用户ID不能为空");
        BusinessException.throwIf(request == null, ResponseCodeEnum.PARAM_ERROR, "编辑用户请求不能为空");

        // 2. 全部字段都为空 → 直接返回（避免无意义的 SQL）
        if (request.isAllNull()) {
            return;
        }

        // 3. 确认目标用户必须存在
        User targetUser = userMapper.selectById(userId);
        BusinessException.throwIf(targetUser == null, ResponseCodeEnum.USER_NOT_EXIST);

        // 4. 安全保护：若要修改角色，禁止把"最后一个管理员"改成非管理员
        if (request.getRole() != null && !PermissionUtil.isAdmin(request.getRole())) {
            // 目标原来是管理员 → 检查改完后还剩几个管理员
            if (targetUser.isAdmin()) {
                long adminCount = countAdminUsers();
                if (adminCount <= 1) {
                    BusinessException.throwOf(ResponseCodeEnum.FAILURE, "必须至少保留一个管理员账号");
                }
            }
        }

        // 5. 组装要更新的 User（null 的字段不会被 MyBatis 动态 SQL 覆盖）
        User patch = new User();
        patch.setId(userId);
        patch.setName(trimToNull(request.getName()));
        patch.setRole(request.getRole());
        patch.setPhone(trimToNull(request.getPhone()));
        patch.setEmail(trimToNull(request.getEmail()));
        patch.setDepartment(trimToNull(request.getDepartment()));
        patch.setMajor(trimToNull(request.getMajor()));
        patch.setStatus(request.getStatus());
        // 如果修改了状态为 0（正常），同时解锁
        if (request.getStatus() != null && request.getStatus() == 0) {
            patch.setLoginFailCount(0);
            patch.setLockTime(null);
        }

        // 6. 执行动态更新
        int rows = userMapper.updateById(patch);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "编辑用户失败");
    }

    /**
     * 字符串 trim 后转 null（空串一律视为 null，避免 MyBatis 动态 SQL 写空串覆盖原值）
     */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ====================================================================================
     * 子模块 2：资源管理（PRD 2.2.2 —— 管理员全局审核 + 增删改查）
     * ==================================================================================== */

    @Override
    public PageResult<ResourceInfoResponse> pageResources(Integer adminRole, ResourceQueryRequest query) {
        // 1. 权限校验：必须管理员
        PermissionUtil.checkGlobalResourcePermission(adminRole);
        if (query == null) {
            query = new ResourceQueryRequest();
        }
        query.normalize();

        // 2. 总数
        long total = resourceMapper.countResources(
                query.getKeyword(),
                query.getCourse(),
                query.getReviewStatus(),
                query.getUploaderId(),
                query.getStartDate(),
                query.getEndDate()
        );
        if (total == 0) {
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }

        // 3. 分页列表
        List<Resource> list = resourceMapper.selectResourcesPage(
                query.getKeyword(),
                query.getCourse(),
                query.getReviewStatus(),
                query.getUploaderId(),
                query.getStartDate(),
                query.getEndDate(),
                query.getOffset(),
                query.getPageSize()
        );

        // 4. 转换为响应 DTO（此处不做 uploaderName 二次查询 join，后续可扩展 LEFT JOIN 或批量查 user 填充）
        List<ResourceInfoResponse> records = list.stream()
                .map(ResourceInfoResponse::fromEntity)
                .collect(Collectors.toList());
        return PageResult.of(records, query.getPageNum(), query.getPageSize(), total);
    }

    @Override
    public ResourceInfoResponse getResourceDetail(Integer adminRole, Long resourceId) {
        PermissionUtil.assertAdmin(adminRole);
        BusinessException.throwIf(resourceId == null || resourceId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "资源ID不能为空");
        Resource r = resourceMapper.selectById(resourceId);
        BusinessException.throwIf(r == null, ResponseCodeEnum.FAILURE, "资源不存在");
        return ResourceInfoResponse.fromEntity(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewResource(Integer adminRole, Long adminUserId, ResourceReviewRequest request) {
        // 1. 权限：必须管理员
        PermissionUtil.checkReviewPermission(adminRole);
        BusinessException.throwIf(request == null, ResponseCodeEnum.PARAM_ERROR, "审核请求不能为空");
        BusinessException.throwIf(adminUserId == null || adminUserId <= 0,
                ResponseCodeEnum.FAILURE, "无法识别审核管理员");

        // 2. 资源必须存在
        Resource r = resourceMapper.selectById(request.getResourceId());
        BusinessException.throwIf(r == null, ResponseCodeEnum.FAILURE, "资源不存在");

        // 3. 拒绝时拒绝理由必填
        BusinessException.throwIf(request.isRejectReasonMissing(),
                ResponseCodeEnum.PARAM_ERROR, "审核拒绝必须填写拒绝理由");

        // 4. 构建更新后的资源对象
        Resource patch = new Resource();
        patch.setId(r.getId());
        if (Boolean.TRUE.equals(request.getApprove())) {
            patch.setReviewStatus(ReviewStatusEnum.APPROVED.getCode());
            patch.setRejectReason(null); // 通过清空旧的拒绝理由
        } else {
            patch.setReviewStatus(ReviewStatusEnum.REJECTED.getCode());
            patch.setRejectReason(trimToNull(request.getRejectReason()));
        }
        patch.setReviewAdminId(adminUserId);
        patch.setReviewTime(LocalDateTime.now());

        // 5. 执行审核更新
        int rows = resourceMapper.updateReviewStatus(patch);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "审核失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Integer adminRole, Long resourceId) {
        PermissionUtil.checkGlobalResourcePermission(adminRole);
        BusinessException.throwIf(resourceId == null || resourceId <= 0,
                ResponseCodeEnum.PARAM_ERROR, "资源ID不能为空");
        Resource r = resourceMapper.selectById(resourceId);
        BusinessException.throwIf(r == null, ResponseCodeEnum.FAILURE, "资源不存在");
        int rows = resourceMapper.deleteById(resourceId);
        BusinessException.throwIf(rows != 1, ResponseCodeEnum.FAILURE, "删除资源失败");
    }

    /* ====================================================================================
     * 子模块 3：数据导入管理（PRD 2.2.2 + 2.4 Excel 批量导入师生信息）
     * ==================================================================================== */

    /**
     * 实现说明：
     * 默认采用「部分成功 + 失败明细」模式（业务上更友好，用户只需修正失败行重新上传）。
     * 如需开启加分项「全量导入回滚」（即任何一行失败则全部不入库）：
     *   在整个 for (ExcelUserRow row : rows) 循环外再追加：
     *     if (importResult.hasError()) throw new BusinessException(...)
     *   即可，因为已加 @Transactional 任何 RuntimeException 都会自动回滚。
     */
    @Override
    public ImportResultResponse importUsers(Integer adminRole, RoleEnum targetRole, MultipartFile file) {
        // 1. 权限 + 参数校验
        PermissionUtil.checkExcelImportPermission(adminRole);
        BusinessException.throwIf(targetRole == null,
                ResponseCodeEnum.PARAM_ERROR, "请指定导入角色类型");
        BusinessException.throwIf(targetRole.isAdmin(),
                ResponseCodeEnum.PARAM_ERROR, "不允许批量导入管理员账号");

        // 2. 解析 Excel
        List<ExcelUserRow> rows;
        try {
            rows = ExcelUtil.parseUserExcel(file, targetRole);
        } catch (IOException e) {
            BusinessException.throwOf(ResponseCodeEnum.FAILURE, "Excel 读取失败：" + e.getMessage());
            return null; // never reach
        } catch (IllegalArgumentException e) {
            BusinessException.throwOf(ResponseCodeEnum.PARAM_ERROR, e.getMessage());
            return null;
        }

        ImportResultResponse importResult = new ImportResultResponse();
        if (rows == null || rows.isEmpty()) {
            importResult.finish(0);
            return importResult;
        }

        final int total = rows.size();

        // 3. 提前取出本文件中所有 username，做 Excel 内重复检测
        Set<String> seenInExcel = new HashSet<>((int) (rows.size() * 1.5) + 1);

        for (ExcelUserRow row : rows) {
            String username = row.getUsername();

            // ========= 校验 1：必填字段完整性（PRD 2.4.2） =========
            if (isBlank(username)) {
                importResult.addError(row.getRowNum(), null, "学号/工号不能为空");
                continue;
            }
            if (isBlank(row.getName())) {
                importResult.addError(row.getRowNum(), username, "姓名不能为空");
                continue;
            }
            if (isBlank(row.getDepartment())) {
                importResult.addError(row.getRowNum(), username, "院系不能为空");
                continue;
            }
            // 学生必须填专业
            if (targetRole.isStudent() && isBlank(row.getMajor())) {
                importResult.addError(row.getRowNum(), username, "学生必填专业");
                continue;
            }
            if (isBlank(row.getPassword())) {
                importResult.addError(row.getRowNum(), username, "初始密码不能为空");
                continue;
            }

            // ========= 校验 2：学号/工号格式（长度+不能过长） =========
            if (username.length() > com.qingzhi.demo.common.Constants.USERNAME_MAX_LENGTH) {
                importResult.addError(row.getRowNum(), username,
                        "账号长度不能超过 " + com.qingzhi.demo.common.Constants.USERNAME_MAX_LENGTH + " 字符");
                continue;
            }

            // ========= 校验 3：Excel 内部重复检查 =========
            if (seenInExcel.contains(username)) {
                importResult.addError(row.getRowNum(), username, "账号在本 Excel 中重复");
                continue;
            }

            // ========= 校验 4：数据库重复检查 =========
            if (userMapper.selectByUsername(username) != null) {
                importResult.addError(row.getRowNum(), username, "账号已在系统中存在");
                continue;
            }

            // ========= 校验 5：密码合法性（PRD 2.4.2：>=8 位且含数字+字母） =========
            try {
                PasswordUtil.validateFormat(row.getPassword());
            } catch (BusinessException be) {
                importResult.addError(row.getRowNum(), username, "初始密码格式不合法：需不少于8位且包含数字和字母");
                continue;
            }

            // ========= 校验 6：手机号（填了则必须合法） =========
            if (!isBlank(row.getPhone())) {
                String phone = row.getPhone();
                if (!phone.matches(com.qingzhi.demo.common.Constants.PHONE_REGEX)) {
                    importResult.addError(row.getRowNum(), username, "手机号格式不正确");
                    continue;
                }
            }
            // ========= 校验 7：邮箱（填了则必须合法） =========
            if (!isBlank(row.getEmail())) {
                String email = row.getEmail();
                if (!email.matches(com.qingzhi.demo.common.Constants.EMAIL_REGEX)) {
                    importResult.addError(row.getRowNum(), username, "邮箱格式不正确");
                    continue;
                }
            }

            // ========= 所有校验通过：加密密码 + 入库 =========
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(PasswordUtil.encrypt(row.getPassword()));
            user.setName(row.getName() == null ? null : row.getName().trim());
            user.setRole(targetRole.getCode());
            user.setStatus(0);
            user.setLoginFailCount(0);
            user.setPhone(trimToNull(row.getPhone()));
            user.setEmail(trimToNull(row.getEmail()));
            user.setDepartment(trimToNull(row.getDepartment()));
            user.setMajor(trimToNull(row.getMajor()));

            try {
                int rowsAffected = userMapper.insert(user);
                if (rowsAffected != 1 || user.getId() == null) {
                    importResult.addError(row.getRowNum(), username, "数据库写入失败");
                    continue;
                }
            } catch (Exception dbEx) {
                importResult.addError(row.getRowNum(), username, "数据库写入异常：" + dbEx.getMessage());
                continue;
            }

            // 成功：加入 seen 集合，后续重复检测用
            seenInExcel.add(username);
        }

        importResult.finish(total);
        return importResult;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
