package com.qingzhi.demo.aspect;

import com.qingzhi.demo.annotation.RateLimit;
import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 限流切面（加分项：文件上传频率限制）
 * <p>实现策略：滑动窗口计数 —— 每个限流 key 维护一个时间戳队列，
 * 每次请求前先清理窗口外的过期时间戳，剩余数量即窗口内已用次数，
 * 超过 maxRequests 则抛出 UPLOAD_RATE_LIMITED。
 */
@Aspect
@Component
public class RateLimitAspect {

    /**
     * <pre>
     * Key: 限流维度 key（USER_xxx / IP_xxx / GLOBAL）
     * Value: 滑动窗口内的请求时间戳队列（毫秒）
     * </pre>
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> windowMap = new ConcurrentHashMap<>();

    /* ====================================================================================
     * 1. AOP 环绕通知：拦截 @RateLimit 注解的方法
     * ==================================================================================== */

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String limitKey = buildLimitKey(rateLimit.dimension());
        int windowMs = rateLimit.windowSeconds() * 1000;
        int maxReq = rateLimit.maxRequests();

        ConcurrentLinkedDeque<Long> queue = windowMap.computeIfAbsent(limitKey,
                k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // 2. 清理窗口外的过期请求（从队头 poll）
        while (!queue.isEmpty() && queue.peekFirst() <= windowStart) {
            queue.pollFirst();
        }

        // 3. 判断是否超限
        if (queue.size() >= maxReq) {
            BusinessException.throwOf(ResponseCodeEnum.UPLOAD_RATE_LIMITED);
            return null; // 不会执行到这里，throwIf 已抛异常
        }

        // 4. 通过限流，记录当前时间戳（队尾入队）
        queue.addLast(now);

        // 5. 执行目标方法
        return pjp.proceed();
    }

    /* ====================================================================================
     * 2. 限流 Key 生成
     * ==================================================================================== */

    private String buildLimitKey(RateLimit.LimitDimension dimension) {
        HttpServletRequest request = getRequest();
        switch (dimension) {
            case USER:
                Long userId = request != null
                        ? (Long) request.getAttribute(Constants.REQUEST_ATTR_CURRENT_USER_ID)
                        : null;
                // 未登录兜底 IP（若业务要求未登录禁止上传，Controller 层会提前拦截）
                return "USER_" + (userId != null ? userId : "ANON_" + getClientIp(request));
            case IP:
                return "IP_" + getClientIp(request);
            case GLOBAL:
            default:
                return "GLOBAL";
        }
    }

    /* ====================================================================================
     * 3. 辅助方法：获取 Request / Client IP
     * ==================================================================================== */

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int comma = ip.indexOf(',');
                return comma < 0 ? ip : ip.substring(0, comma).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
