package com.rxas400adm.common.exception;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 当前 profile 判定（统一收敛到 ProfileResolver）：dev/mock/test 回显异常内部细节，其余按生产处理 */
    private final ProfileResolver profileResolver;

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("Validation failed");
        return ApiResponse.error(400, msg);
    }

    /**
     * 【E9】参数约束违反：不再回显 e.getMessage() 原文（可能含属性路径/非法值等内部细节），
     * 只取首个约束的简洁文案返回，完整信息留 warn 日志；HTTP 状态语义保持 400 不变
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        log.warn("参数约束违反: {}", e.getMessage());
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Validation failed");
        return ApiResponse.error(400, msg);
    }

    /** Missing required request parameter */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.error(400, "Missing required parameter: " + e.getParameterName());
    }

    /** 请求体不可解析（向 @RequestParam 接口误发 JSON body、非法 JSON）或参数类型不匹配：一律 400 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleBadRequest(Exception e) {
        log.warn("请求格式错误: {}", e.getMessage());
        return ApiResponse.error(400, "Invalid request parameter format");
    }

    /** 静态资源 / 路由未命中：显式 404，避免落入兜底 500（B4） */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResource(NoResourceFoundException e) {
        return ApiResponse.error(404, "Resource not found");
    }

    /** 请求方法不被允许（如 GET 命中仅 POST 的端点）：显式 405，避免落入兜底 500（B4） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ApiResponse.error(405, "Request method not allowed");
    }

    /** 【E10】异步请求超时（DeferredResult/流式响应超时未完成）：显式 503 + 固定文案，避免落入兜底 500 */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleAsyncTimeout(AsyncRequestTimeoutException e) {
        log.warn("异步请求超时: {}", e.getMessage());
        return ApiResponse.error(503, "Request timeout, please retry");
    }

    /** 【E10】Content-Type 不被接口支持：显式 415，避免落入兜底 500 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type: {}", e.getContentType());
        return ApiResponse.error(415, "Unsupported Content-Type");
    }

    /** 上传文件超限（spring.servlet.multipart 上限）：转 400 友好提示，避免落成 500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("上传文件超限: {}", e.getMessage());
        return ApiResponse.error(400, "Upload file exceeds size limit");
    }

    /** @PreAuthorize 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.error(403, "Access denied");
    }

    /** 认证失败（Service/Controller 层抛 AuthenticationException 时不再落兜底 500） */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ApiResponse.error(401, "Not authenticated or session expired");
    }

    /** 数据库访问异常（SQL 语法、连接、超时等）：统一返回友好提示，不暴露内部 SQL 细节 */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccess(DataAccessException e) {
        log.error("数据库访问异常", e);
        return ApiResponse.error(500, "Database error, please try again later or contact administrator");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突: {}", e.getMessage());
        return ApiResponse.error(409, "Data already exists, please do not submit repeatedly");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        // P1-9：白名单方式——仅开发环境（dev/mock/test）回显底层异常消息（SQL/路径/库名等），
        // 其余环境一律只返回固定文案，细节进日志，避免 uat/生产泄露内部信息
        // P3-5：default/未指定 profile 不再按开发处理（避免生产未显式配置时误回显内部细节）
        if (showInternalDetail()) {
            return ApiResponse.error(500, "Internal system error: " + e.getMessage());
        }
        return ApiResponse.error(500, "Internal system error");
    }

    /** 开发环境白名单：仅显式 dev / mock / test；default 或未指定 profile 一律不回显 */
    private boolean showInternalDetail() {
        return profileResolver.isDevLikeMode();
    }
}