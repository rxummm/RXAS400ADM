package com.rxas400adm.common.exception;

import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 当前 profile（逗号分隔）：仅在 dev/mock/test 等开发环境回显异常内部细节（S5/P1-9）。
     *  默认空串：未显式配置 profile 时按生产处理，不回显内部细节（P3-5）。 */
    @Value("${spring.profiles.active:}")
    private String activeProfile;

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
                .orElse("参数校验失败");
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    /** 缺失必填请求参数（@RequestParam 未传 / 传错位置）：客户端请求畸形，按 400 处理，避免落兜底 500 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.error(400, "缺少请求参数: " + e.getParameterName());
    }

    /** 请求体不可解析（向 @RequestParam 接口误发 JSON body、非法 JSON）或参数类型不匹配：一律 400 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleBadRequest(Exception e) {
        log.warn("请求格式错误: {}", e.getMessage());
        return ApiResponse.error(400, "请求参数格式错误");
    }

    /** 上传文件超限（spring.servlet.multipart 上限）：转 400 友好提示，避免落成 500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("上传文件超限: {}", e.getMessage());
        return ApiResponse.error(400, "上传文件超出大小限制");
    }

    /** @PreAuthorize 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.error(403, "无权限访问");
    }

    /** 认证失败（Service/Controller 层抛 AuthenticationException 时不再落兜底 500） */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ApiResponse.error(401, "未认证或会话已过期");
    }

    /** 数据库访问异常（SQL 语法、连接、超时等）：统一返回友好提示，不暴露内部 SQL 细节 */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccess(DataAccessException e) {
        log.error("数据库访问异常", e);
        return ApiResponse.error(500, "数据查询失败，请稍后重试或联系管理员");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        // P1-9：白名单方式——仅开发环境（dev/mock/test）回显底层异常消息（SQL/路径/库名等），
        // 其余环境一律只返回固定文案，细节进日志，避免 uat/生产泄露内部信息
        // P3-5：default/未指定 profile 不再按开发处理（避免生产未显式配置时误回显内部细节）
        if (showInternalDetail()) {
            return ApiResponse.error(500, "系统内部错误: " + e.getMessage());
        }
        return ApiResponse.error(500, "系统内部错误");
    }

    /** 开发环境白名单：仅显式 dev / mock / test；default 或未指定 profile 一律不回显 */
    private boolean showInternalDetail() {
        if (activeProfile == null || activeProfile.isBlank()) {
            return false;
        }
        return java.util.Arrays.stream(activeProfile.split(","))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "mock".equalsIgnoreCase(p)
                        || "test".equalsIgnoreCase(p));
    }
}