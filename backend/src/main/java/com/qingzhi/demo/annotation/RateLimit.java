package com.qingzhi.demo.annotation;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.enums.ResponseCodeEnum;

import java.lang.annotation.*;

/**
 * 接口限流注解（加分项：文件上传频率限制 / 下载刷接口拦截）
 * <p>默认：同一用户每分钟最多 5 次（Constants.FILE_UPLOAD_RATE_LIMIT / FILE_UPLOAD_RATE_WINDOW_SECONDS）。
 * <p>实现方式：基于 JVM 内存 + 滑动窗口计数器（单实例部署足够；集群部署可改为 Redis 实现）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度
     * <p>USER：按用户 ID 限流（默认，从 request.getAttribute(CURRENT_USER_ID) 取）<br>
     * IP：按客户端 IP 限流<br>
     * GLOBAL：全局限流（忽略身份）
     */
    LimitDimension dimension() default LimitDimension.USER;

    /**
     * 时间窗口大小（秒），默认 60 秒
     */
    int windowSeconds() default Constants.FILE_UPLOAD_RATE_WINDOW_SECONDS;

    /**
     * 窗口内最大访问次数，默认 5 次
     */
    int maxRequests() default Constants.FILE_UPLOAD_RATE_LIMIT;

    /**
     * 是否对管理员（role=0）豁免 —— true=管理员访问直接放行，不计入滑动窗口
     * <p>默认 true（下载刷接口拦截场景：管理员不触发；上传频率场景可显式传 false 覆盖）
     */
    boolean skipForAdmin() default true;

    /**
     * 自定义业务错误码（不填=用默认 UPLOAD_RATE_LIMITED=5003 上传频率超限）
     * <p>下载场景传 OPERATION_TOO_FREQUENT=5007 提示「操作频繁」，与上传频率区分友好提示
     */
    ResponseCodeEnum errorCode() default ResponseCodeEnum.UPLOAD_RATE_LIMITED;

    enum LimitDimension {
        USER, IP, GLOBAL
    }
}
