package com.rxas400adm.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * C-SEC004: 客户端 IP 解析统一工具。
 * <p>策略（S3）：仅当请求直接来自可信反向代理时才信任 X-Forwarded-For，
 * 否则一律取 remoteAddr——防伪造头绕过限流/审计。
 * 原三处重复实现（AuthController.clientIp / RateLimitFilter.getClientIp /
 * OperateLogAspect.currentIp）收敛至此。
 */
@Component
public class ClientIpResolver {

    @Value("${rxas400.security.trusted-proxies:}")
    private String trustedProxies;

    /**
     * 解析客户端真实 IP。
     * 优先级：X-Forwarded-For（首个 IP） → X-Real-IP → remoteAddr。
     * 仅当 remoteAddr 命中可信代理白名单时才采信转发头。
     */
    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isTrustedProxy(remote)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remote;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || !StringUtils.hasText(trustedProxies)) {
            return false;
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .anyMatch(p -> "*".equals(p) || p.equalsIgnoreCase(remoteAddr));
    }
}
