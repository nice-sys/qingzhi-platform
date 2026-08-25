package com.qingzhi.demo.exception;

import com.qingzhi.demo.enums.ResponseCodeEnum;

/**
 * 自定义业务异常
 * <p>用于封装业务逻辑中产生的异常，携带错误码与提示信息，由 GlobalExceptionHandler 统一捕获处理</p>
 *
 * @see GlobalExceptionHandler
 * @see ResponseCodeEnum
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码（对应 ResponseCodeEnum.code）
     */
    private final Integer code;

    /**
     * 错误提示信息
     */
    private final String message;

    /**
     * 构造业务异常（仅提示信息，使用通用失败码 0）
     *
     * @param message 错误提示信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResponseCodeEnum.FAILURE.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（基于响应码枚举）
     *
     * @param codeEnum 响应码枚举
     */
    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMessage());
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
    }

    /**
     * 构造业务异常（基于响应码枚举 + 自定义提示信息）
     *
     * @param codeEnum 响应码枚举（取其 code）
     * @param message  自定义提示信息（覆盖枚举中的 message）
     */
    public BusinessException(ResponseCodeEnum codeEnum, String message) {
        super(message);
        this.code = codeEnum.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（自定义 code + message）
     *
     * @param code    错误码
     * @param message 错误提示信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（携带原始异常栈）
     *
     * @param codeEnum 响应码枚举
     * @param cause    原始异常
     */
    public BusinessException(ResponseCodeEnum codeEnum, Throwable cause) {
        super(codeEnum.getMessage(), cause);
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
    }

    /**
     * 快速抛出业务异常（基于响应码枚举）
     * <p>使用方式：BusinessException.throwOf(ResponseCodeEnum.RESOURCE_NOT_FOUND)</p>
     *
     * @param codeEnum 响应码枚举
     */
    public static void throwOf(ResponseCodeEnum codeEnum) {
        throw new BusinessException(codeEnum);
    }

    /**
     * 快速抛出业务异常（响应码枚举 + 自定义提示）
     *
     * @param codeEnum 响应码枚举
     * @param message  自定义提示信息
     */
    public static void throwOf(ResponseCodeEnum codeEnum, String message) {
        throw new BusinessException(codeEnum, message);
    }

    /**
     * 根据条件抛出业务异常（断言风格，当 condition == true 时抛出）
     *
     * @param condition 触发条件
     * @param codeEnum  响应码枚举
     */
    public static void throwIf(boolean condition, ResponseCodeEnum codeEnum) {
        if (condition) {
            throwOf(codeEnum);
        }
    }

    /**
     * 根据条件抛出业务异常（断言风格，当 condition == true 时抛出，带自定义提示）
     *
     * @param condition 触发条件
     * @param codeEnum  响应码枚举
     * @param message   自定义提示信息
     */
    public static void throwIf(boolean condition, ResponseCodeEnum codeEnum, String message) {
        if (condition) {
            throwOf(codeEnum, message);
        }
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
