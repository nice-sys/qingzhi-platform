package com.qingzhi.demo.annotation;

import com.qingzhi.demo.common.Constants;

import java.lang.annotation.*;

/**
 * 接口限流注解（加分项：文件上传频率限制）
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
     * 窗口内最大访问次数，默认 5 次（PRD：同一用户每分钟最多上传 5 个文件）
     */
    int maxRequests() default Constants.FILE_UPLOAD_RATE_LIMIT;

    enum LimitDimension {
        USER, IP, GLOBAL
    }
}
