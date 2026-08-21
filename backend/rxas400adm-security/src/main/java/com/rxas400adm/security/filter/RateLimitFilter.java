package com.rxas400adm.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 请求限流过滤器（Bucket4j）：
 * - 登录端点：5 次/分钟（防暴力破解）
 * - 命令执行端点：10 次/分钟（防滥用）
 * - 通用 API：60 次/分钟（防洪泛）
 * - 超限返回 429 Too Many Requests
 *
 * 使用 ConcurrentHashMap 存储桶，按 IP 分组。
 * 生产环境建议替换为 Redis 分布式桶（bucket4j-redis）。
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rxas400.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rxas400.security.rate-limit.login-per-minute:5}")
    private int loginPerMinute;

    @Value("${rxas400.security.rate-limit.command-per-minute:10}")
    private int commandPerMinute;

    @Value("${rxas400.security.rate-limit.api-per-minute:60}")
    private int apiPerMinute;

    /** 桶缓存：key = "ip:pathCategory"，value = Bucket */
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String category = resolveCategory(path);

        String key = ip + ":" + category;
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(category));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("[限流] IP {} 触发限流 (category={}, path={})", ip, category, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String resolveCategory(String path) {
        if (path.startsWith("/api/v1/auth/login")) {
            return "login";
        }
        if (path.startsWith("/api/v1/as400/commands")) {
            return "command";
        }
        return "api";
    }

    private Bucket createBucket(String category) {
        int permits;
        Duration period;
        switch (category) {
            case "login" -> {
                permits = loginPerMinute;
                period = Duration.ofMinutes(1);
            }
            case "command" -> {
                permits = commandPerMinute;
                period = Duration.ofMinutes(1);
            }
            default -> {
                permits = apiPerMinute;
                period = Duration.ofMinutes(1);
            }
        }
        Bandwidth bandwidth = Bandwidth.classic(permits, Refill.greedy(permits, period));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
