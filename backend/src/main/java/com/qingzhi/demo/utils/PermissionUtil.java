package com.qingzhi.demo.utils;

import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;

/**
 * 权限判断工具类
 * <p>对应 PRD 2.2.1 角色定义与权限矩阵：
 * 将角色校验逻辑集中封装，避免 Service 层大量 if/else。</p>
 *
 * <p>权限矩阵摘要：</p>
 * <pre>
 * 功能点                     管理员(0)  教师(1)  学生(2)
 * 用户信息增删改查（所有用户）    Y         -        -
 * 重置用户密码                   Y         -        -
 * 资源审核（通过/拒绝）           Y         -        -
 * 资源增删改查（所有资源）        Y         -        -
 * Excel 批量导入师生信息         Y         -        -
 * 发布/删除/修改自己的资源       -         Y        Y
 * 查看所有已通过资源             Y         Y        Y
 * 收藏/取消收藏资源              -         Y        Y
 * 查询/补充个人信息              -         Y        Y
 * 修改自己的密码                 Y         Y        Y
 * 查看自己发布的资源（分页）     -         Y        Y
 * 查看自己的收藏列表（分页）     -         Y        Y
 * </pre>
 */
public final class PermissionUtil {

    private PermissionUtil() {
        throw new UnsupportedOperationException("PermissionUtil 不可实例化");
    }

    /* ====================================================================================
     * 一、静态角色判断（返回 boolean，不抛异常）
     * ==================================================================================== */

    public static boolean isAdmin(Integer role) {
        return RoleEnum.ADMIN.getCode() == (role == null ? -1 : role);
    }

    public static boolean isTeacher(Integer role) {
        return RoleEnum.TEACHER.getCode() == (role == null ? -1 : role);
    }

    public static boolean isStudent(Integer role) {
        return RoleEnum.STUDENT.getCode() == (role == null ? -1 : role);
    }

    /**
     * 是否为普通用户（教师或学生，非管理员）
     */
    public static boolean isNormalUser(Integer role) {
        return isTeacher(role) || isStudent(role);
    }

    /**
     * 是否为有效角色（0/1/2）
     */
    public static boolean isValidRole(Integer role) {
        return RoleEnum.of(role) != null;
    }

    /* ====================================================================================
     * 二、断言式权限校验（不满足条件直接抛出 1002 NO_PERMISSION 异常）
     *    适合在 Service 层/Controller 层入口处直接调用，一行搞定权限判定
     * ==================================================================================== */

    /**
     * 断言：必须是管理员（用于"管理员专属"功能入口）
     * <p>PRD 权限矩阵对应：用户管理、重置密码、资源审核、资源全局管理、Excel导入</p>
     */
    public static void assertAdmin(Integer role) {
        if (!isAdmin(role)) {
            BusinessException.throwOf(ResponseCodeEnum.NO_PERMISSION);
        }
    }

    /**
     * 断言：必须是教师或学生（普通用户）
     * <p>用于"个人自用"的接口（即使是管理员想用也需使用普通接口或管理员接口，防止混淆）</p>
     */
    public static void assertNormalUser(Integer role) {
        if (!isNormalUser(role)) {
            BusinessException.throwOf(ResponseCodeEnum.NO_PERMISSION);
        }
    }

    /**
     * 断言：必须是合法角色编码（0/1/2）
     */
    public static void assertValidRole(Integer role) {
        if (!isValidRole(role)) {
            BusinessException.throwOf(ResponseCodeEnum.FAILURE, "无效的角色编码");
        }
    }

    /* ====================================================================================
     * 三、资源级别权限校验（更细粒度，如"自己的资源" vs "他人的资源"）
     * ==================================================================================== */

    /**
     * 断言：要么是管理员，要么是自己本人（用于"查看/修改/删除 自己的资料/资源"场景）
     *
     * @param currentUserId    当前登录用户ID
     * @param currentUserRole  当前登录用户角色
     * @param resourceOwnerId  资源所有者用户ID / 被操作的用户ID
     * @throws BusinessException 无权限时抛出 1002 NO_PERMISSION
     */
    public static void assertAdminOrSelf(Long currentUserId, Integer currentUserRole, Long resourceOwnerId) {
        if (isAdmin(currentUserRole)) {
            return; // 管理员放行
        }
        if (currentUserId != null && resourceOwnerId != null && currentUserId.equals(resourceOwnerId)) {
            return; // 本人放行
        }
        BusinessException.throwOf(ResponseCodeEnum.NO_PERMISSION);
    }

    /**
     * 断言：只能操作自己的资源（教师/学生）
     * <p>用于"发布/删除/修改自己的资源"，管理员如需操作请走管理员接口</p>
     */
    public static void assertSelf(Long currentUserId, Long resourceOwnerId) {
        if (currentUserId == null || resourceOwnerId == null || !currentUserId.equals(resourceOwnerId)) {
            BusinessException.throwOf(ResponseCodeEnum.NO_PERMISSION);
        }
    }

    /* ====================================================================================
     * 四、按 PRD 功能封装的语义化权限校验（与权限矩阵一一对应，代码可读性高）
     * ==================================================================================== */

    /** 用户管理（增删改查所有用户）——仅管理员 */
    public static void checkUserManagePermission(Integer role) { assertAdmin(role); }

    /** 重置用户密码 ——仅管理员 */
    public static void checkResetPasswordPermission(Integer role) { assertAdmin(role); }

    /** 资源审核（通过/拒绝）——仅管理员 */
    public static void checkReviewPermission(Integer role) { assertAdmin(role); }

    /** 全局资源管理（增删改查所有资源）——仅管理员 */
    public static void checkGlobalResourcePermission(Integer role) { assertAdmin(role); }

    /** Excel 批量导入师生信息 ——仅管理员 */
    public static void checkExcelImportPermission(Integer role) { assertAdmin(role); }

    /** 发布/删除/修改自己的资源 ——教师或学生（需配合 assertSelf 判断是否本人） */
    public static void checkResourceOperatePermission(Integer role) {
        if (!isNormalUser(role) && !isAdmin(role)) {
            BusinessException.throwOf(ResponseCodeEnum.NO_PERMISSION);
        }
    }

    /** 查看所有已通过资源 ——全员（管理员/教师/学生） */
    public static void checkViewPublishedResourcePermission(Integer role) {
        // 三个角色均允许，非法角色抛错
        assertValidRole(role);
    }

    /** 收藏/取消收藏资源 ——教师或学生 */
    public static void checkFavoritePermission(Integer role) {
        assertNormalUser(role);
    }

    /** 查询/补充个人信息 ——全员（管理员也能看自己的） */
    public static void checkViewPersonalInfoPermission(Integer role) { assertValidRole(role); }

    /** 修改自己的密码 ——全员（含管理员本人，此时通过自用改密接口） */
    public static void checkChangeOwnPasswordPermission(Integer role) { assertValidRole(role); }

    /** 查看自己发布的资源 ——教师或学生 */
    public static void checkViewOwnResourcesPermission(Integer role) { assertNormalUser(role); }

    /** 查看自己的收藏列表 ——教师或学生 */
    public static void checkViewOwnFavoritesPermission(Integer role) { assertNormalUser(role); }
}
