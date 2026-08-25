package com.qingzhi.demo.common;

import com.qingzhi.demo.enums.ResponseCodeEnum;

import java.io.Serializable;

/**
 * 统一响应封装（code + message + data）
 * <p>对应 PRD 5.1 统一响应格式，所有接口均返回该结构的 JSON</p>
 *
 * @param <T> 业务数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：1 表示成功，其他表示失败（见错误码表） */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据：Object / Array / null */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（不带业务数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带业务数据）
     *
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义提示信息 + 业务数据）
     *
     * @param message 提示信息
     * @param data    业务数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（默认通用失败码）
     */
    public static <T> Result<T> fail() {
        return fail(ResponseCodeEnum.FAILURE);
    }

    /**
     * 失败响应（指定响应码枚举）
     *
     * @param codeEnum 响应码枚举
     */
    public static <T> Result<T> fail(ResponseCodeEnum codeEnum) {
        return new Result<>(codeEnum.getCode(), codeEnum.getMessage(), null);
    }

    /**
     * 失败响应（指定响应码枚举 + 自定义提示信息）
     *
     * @param codeEnum 响应码枚举
     * @param message  自定义提示信息
     */
    public static <T> Result<T> fail(ResponseCodeEnum codeEnum, String message) {
        return new Result<>(codeEnum.getCode(), message, null);
    }

    /**
     * 失败响应（自定义状态码 + 提示信息）
     *
     * @param code    状态码
     * @param message 提示信息
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断当前响应是否成功
     */
    public boolean isSuccess() {
        return code != null && code == ResponseCodeEnum.SUCCESS.getCode();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
