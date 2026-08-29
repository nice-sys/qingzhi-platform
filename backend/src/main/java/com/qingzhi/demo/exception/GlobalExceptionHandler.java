package com.qingzhi.demo.exception;

import com.qingzhi.demo.common.Result;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>通过 @RestControllerAdvice + @ExceptionHandler 实现集中捕获所有异常，
 * 统一转换为 Result 格式的 JSON 返回，避免将堆栈信息暴露给前端。</p>
 * <p>对应 PRD 附录一：全局异常处理</p>
 *
 * @see Result
 * @see BusinessException
 * @see ResponseCodeEnum
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ====================================================================================
     * 一、业务异常处理（自定义 BusinessException）
     * ==================================================================================== */

    /**
     * 处理自定义业务异常
     * <p>业务代码中主动抛出的 BusinessException，直接携带 code + message 返回</p>
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /* ====================================================================================
     * 二、参数校验相关异常
     * ==================================================================================== */

    /**
     * 处理 @Valid 注解在 @RequestBody 上触发的校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败-RequestBody] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /**
     * 处理表单绑定（非 JSON）的参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败-表单] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /**
     * 处理 @Validated 注解在方法参数上（@RequestParam / @PathVariable）触发的校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败-方法参数] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = "缺少必填参数：" + e.getParameterName();
        log.warn("[缺少请求参数] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /**
     * 处理请求体解析异常（如 JSON 格式错误、必填字段缺失等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求体解析失败，请检查 JSON 格式是否正确";
        log.warn("[请求体解析失败] {}", e.getMessage());
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /* ====================================================================================
     * 三、文件上传相关异常
     * ==================================================================================== */

    /**
     * 处理文件上传大小超限异常（对应 PRD 5001 错误码）
     * <p>当上传的文件超过 application.yml 中配置的 max-file-size（100MB）时触发</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[文件大小超限] {}", e.getMessage());
        return Result.fail(ResponseCodeEnum.FILE_SIZE_EXCEEDED);
    }

    /* ====================================================================================
     * 四、请求方法 / 媒体类型 不支持
     * ==================================================================================== */

    /**
     * 处理 HTTP 请求方法不支持异常（如 GET 接口用 POST 请求）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = "不支持的请求方法：" + e.getMethod() + "，请使用 " + e.getSupportedHttpMethods();
        log.warn("[请求方法不支持] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /**
     * 处理 HTTP 媒体类型不支持异常（如 Content-Type 错误）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = "不支持的 Content-Type：" + e.getContentType();
        log.warn("[媒体类型不支持] {}", message);
        return Result.fail(ResponseCodeEnum.FAILURE, message);
    }

    /* ====================================================================================
     * 五、常见运行时异常
     * ==================================================================================== */

    /**
     * 处理非法参数异常（手动调用时参数不合法）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[非法参数] {}", e.getMessage());
        return Result.fail(ResponseCodeEnum.FAILURE, e.getMessage());
    }

    /**
     * 处理空指针异常（避免堆栈信息暴露给前端）
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("[空指针异常] ", e);
        return Result.fail(ResponseCodeEnum.FAILURE, "系统繁忙，请稍后重试");
    }

    /* ====================================================================================
     * 六、兜底异常处理（捕获所有未被上面匹配的 Exception）
     * ==================================================================================== */

    /**
     * 兜底异常处理：捕获所有 Exception，防止未知异常直接暴露给前端
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[系统异常] 未捕获的异常：", e);
        return Result.fail(ResponseCodeEnum.FAILURE);
    }
}
