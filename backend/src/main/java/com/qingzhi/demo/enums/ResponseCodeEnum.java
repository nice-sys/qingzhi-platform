package com.qingzhi.demo.enums;

/**
 * 统一响应码枚举
 * <p>对应 PRD 5.3 错误码定义：code = 1 表示成功，其他表示失败</p>
 */
public enum ResponseCodeEnum {

    /** 成功：请求处理成功 */
    SUCCESS(1, "Success!"),

    /** 通用失败：未知或未归类错误 */
    FAILURE(0, "系统繁忙，请稍后重试"),

    /** 未登录：未携带有效 JWT 或 Token 过期 */
    NOT_LOGGED_IN(1001, "未登录或登录已过期"),

    /** 无权限：当前角色无权执行该操作 */
    NO_PERMISSION(1002, "无权限执行该操作"),

    /** 账号已存在：注册时学号/工号重复 */
    ACCOUNT_ALREADY_EXISTS(2001, "账号已存在"),

    /** 账号或密码错误：登录凭证校验失败 */
    ACCOUNT_OR_PASSWORD_ERROR(2002, "账号或密码错误"),

    /** 账号已锁定：登录失败次数超限，账号被锁定 */
    ACCOUNT_LOCKED(2003, "账号已锁定，请稍后再试"),

    /** 密码格式不合法：密码不满足 >=8位且含数字+字母 */
    INVALID_PASSWORD_FORMAT(2004, "密码格式不合法：需不少于8位且包含数字和字母"),

    /** 资源不存在：操作的资源 ID 无效 */
    RESOURCE_NOT_FOUND(3001, "资源不存在"),

    /** 资源状态不允许该操作：如对已通过资源直接发布 */
    RESOURCE_STATUS_NOT_ALLOWED(3002, "当前资源状态不允许该操作"),

    /** Excel 格式错误：上传的文件不符合规范 */
    EXCEL_FORMAT_ERROR(4001, "Excel 格式错误"),

    /** Excel 数据校验失败：学号/工号格式错误、重复、必填字段缺失等 */
    EXCEL_DATA_INVALID(4002, "Excel 数据校验失败"),

    /** 文件大小超限：单文件超过 50MB */
    FILE_SIZE_EXCEEDED(5001, "文件大小超限（单个文件不超过 50MB）"),

    /** 文件格式不支持：上传了不支持的文件类型 */
    FILE_TYPE_NOT_SUPPORTED(5002, "文件格式不支持"),

    /** 上传频率超限：每分钟上传超过 5 个文件 */
    UPLOAD_RATE_LIMITED(5003, "上传频率超限，请稍后再试"),

    /** 参数错误：请求参数为空或非法 */
    PARAM_ERROR(2, "参数错误"),

    /** 用户不存在：查询或操作的用户ID无效 */
    USER_NOT_EXIST(2005, "用户不存在"),

    /** 账号已存在（别名：同 ACCOUNT_ALREADY_EXISTS，方便管理员创建用户时语义化使用） */
    USERNAME_EXIST(2001, "账号已存在");

    /** 状态码：1 表示成功，其他表示失败 */
    private final int code;

    /** 提示信息 */
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
