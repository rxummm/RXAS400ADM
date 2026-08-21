package com.rxas400adm.system.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * @OperateLog 注解切面：方法执行后写入 rx_audit_log（审计模块）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /** 可信反向代理 IP 列表（逗号分隔，S3）；留空则完全忽略 X-Forwarded-For */
    @Value("${rxas400.security.trusted-proxies:}")
    private String trustedProxies;

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            try {
                saveLog(joinPoint, operateLog, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("写入审计日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperateLog operateLog, long costMs) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserName(currentUsername());
        auditLog.setModule(operateLog.module());
        auditLog.setAction(operateLog.operation());
        auditLog.setTarget(buildTarget(joinPoint));
        auditLog.setIp(currentIp());
        auditLog.setDetail(buildDetail(joinPoint));
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    private String buildTarget(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }

    /** 敏感字段名匹配（大小写不敏感）：命中则审计详情中脱敏为 *** */
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i).*(password|passwd|pwd|secret|token|apikey|api_key|privatekey|authorization).*");
    private static final String REDACTED = "***";

    /** N3：原始 String 参数（如 logout 的 Authorization 头）中的 Bearer token 脱敏 */
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(Bearer\\s+)([^\\s]+)");

    private String buildDetail(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            // 只序列化简单参数（DTO 等），避免序列化 HttpServletRequest
            Object detail = Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .findFirst().orElse(null);
            if (detail == null) {
                return null;
            }
            // N3：原始 String 参数（如 logout 的 Authorization 头）直接文本处理，
            // 否则 valueToTree 生成的文本节点不会被递归脱敏，完整 JWT 会落入审计库
            if (detail instanceof String) {
                return BEARER_TOKEN.matcher((String) detail).replaceAll("$1" + REDACTED);
            }
            // P0-4：序列化后递归脱敏，改密/重置密码等请求不再把明文密码写入审计日志
            JsonNode node = objectMapper.valueToTree(detail);
            redact(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(args[0]);
        }
    }

    /** 递归脱敏：字段名命中敏感模式则把值替换为 ***（Map / POJO / 嵌套对象均覆盖） */
    private void redact(JsonNode node) {
        if (node == null || !node.isContainerNode()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEY.matcher(field.getKey()).matches()) {
                    objectNode.put(field.getKey(), REDACTED);
                } else {
                    redact(field.getValue());
                }
            }
        } else {
            node.forEach(this::redact);
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    /**
     * S3：与 AuthController.clientIp 一致——仅当请求直接来自可信反向代理时才信任 X-Forwarded-For，
     * 避免伪造头污染审计 IP。
     */
    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String remote = request.getRemoteAddr();
        if (remote != null && StringUtils.hasText(trustedProxies)) {
            boolean trusted = Arrays.stream(trustedProxies.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isBlank())
                    .anyMatch(p -> "*".equals(p) || p.equalsIgnoreCase(remote));
            if (trusted) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) {
                    return ip.split(",")[0].trim();
                }
            }
        }
        return remote;
    }
}
