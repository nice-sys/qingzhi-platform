package com.qingzhi.demo.common;

/**
 * 全局常量定义
 * <p>集中管理项目中的所有魔法值，避免在代码中硬编码</p>
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Constants 类不可实例化");
    }

    /* ====================================================================================
     * 分页相关常量
     * ==================================================================================== */

    /**
     * 默认当前页码
     */
    public static final Integer DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页条数
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 每页最大条数（防止一次查询过多数据）
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    /* ====================================================================================
     * 文件上传相关常量
     * ==================================================================================== */

    /**
     * 文件上传最大大小（字节）：100MB（与 application.yml spring.servlet.multipart.max-file-size 对齐）
     */
    public static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;

    /**
     * 单个用户每日最多上传的资源数（草稿 + 正式资源 合并计数，每条 resource 行占 1 个配额）
     */
    public static final int DAILY_UPLOAD_MAX_COUNT = 100;

    /**
     * 文件上传频率限制：同一用户每分钟最多上传的文件数（对应 PRD 加分项：物理上传频率拦截）
     */
    public static final int FILE_UPLOAD_RATE_LIMIT = 5;

    /**
     * 文件上传限流时间窗口（秒）：60秒 = 1分钟
     */
    public static final int FILE_UPLOAD_RATE_WINDOW_SECONDS = 60;

    /**
     * 允许的文件扩展名（常见文档 + 图片格式）
     * 对应 PRD 2.3.1：支持 PDF / PPT / Word / 图片等常见格式
     */
    public static final String[] ALLOWED_FILE_EXTENSIONS = {
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    };

    /* ====================================================================================
     * 用户与认证相关常量
     * ==================================================================================== */

    /**
     * 内置管理员账号
     */
    public static final String ADMIN_USERNAME = "Admin";

    /**
     * 内置管理员初始密码
     */
    public static final String ADMIN_INITIAL_PASSWORD = "Admin2026";

    /**
     * JWT Token 在请求 Header 中的名称
     */
    public static final String JWT_HEADER_NAME = "Authorization";

    /**
     * JWT Token 前缀（Bearer Token）
     */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * 登录失败次数阈值：同一账号 5 分钟内连续失败 5 次触发锁定（PRD 加分项）
     */
    public static final int LOGIN_FAIL_THRESHOLD = 5;

    /**
     * 登录失败计数时间窗口（分钟）：5分钟
     */
    public static final int LOGIN_FAIL_WINDOW_MINUTES = 5;

    /**
     * 账号锁定时长（分钟）：15分钟
     */
    public static final int ACCOUNT_LOCK_DURATION_MINUTES = 15;

    /**
     * 密码最小长度（PRD 2.1.1：>= 8位）
     */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * 请求属性中存储当前登录用户ID的 key
     * 由 JwtInterceptor 解析 Token 后存入
     */
    public static final String REQUEST_ATTR_CURRENT_USER_ID = "currentUserId";

    /**
     * 请求属性中存储当前登录用户角色的 key
     */
    public static final String REQUEST_ATTR_CURRENT_USER_ROLE = "currentUserRole";

    /* ====================================================================================
     * 数据库字段长度相关常量
     * ==================================================================================== */

    /**
     * 用户名最大长度（学号/工号/Admin）
     */
    public static final int USERNAME_MAX_LENGTH = 50;

    /**
     * 姓名最大长度
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * 手机号长度
     */
    public static final int PHONE_LENGTH = 11;

    /**
     * 邮箱最大长度
     */
    public static final int EMAIL_MAX_LENGTH = 100;

    /**
     * 院系/专业最大长度
     */
    public static final int DEPT_MAX_LENGTH = 100;

    /**
     * 资源标题最大长度
     */
    public static final int RESOURCE_TITLE_MAX_LENGTH = 200;

    /**
     * 所属课程最大长度
     */
    public static final int COURSE_MAX_LENGTH = 100;

    /**
     * 拒绝理由最大长度
     */
    public static final int REJECT_REASON_MAX_LENGTH = 500;

    /**
     * 文件路径最大长度
     */
    public static final int FILE_PATH_MAX_LENGTH = 500;

    /**
     * 文件名最大长度
     */
    public static final int FILE_NAME_MAX_LENGTH = 200;

    /**
     * 文件哈希（MD5 = 32位 / SHA-256 = 64位，统一取 64）
     */
    public static final int FILE_HASH_MAX_LENGTH = 64;

    /* ====================================================================================
     * 正则表达式
     * ==================================================================================== */

    /**
     * 密码格式正则：必须同时包含数字和字母，长度 >= PASSWORD_MIN_LENGTH
     */
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-zA-Z]).{" + PASSWORD_MIN_LENGTH + ",}$";

    /**
     * 手机号正则（中国大陆 11 位手机号）
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 邮箱正则
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
}
