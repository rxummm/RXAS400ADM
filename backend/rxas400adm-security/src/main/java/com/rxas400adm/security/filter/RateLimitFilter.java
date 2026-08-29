package com.rxas400adm.security.filter;

import com.rxas400adm.security.config.ProxyProperties;
import com.rxas400adm.security.config.RateLimitProperties;
import com.rxas400adm.system.service.SysConfigService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 请求限流过滤器（Bucket4j）：
 * - 登录端点：5 次/分钟（防暴力破解）
 * - 命令执行端点：10 次/分钟（防滥用）
 * - 通用 API：60 次/分钟（防洪泛）
 * - 超限返回 429 Too Many Requests
 *
 * <p>C4：桶缓存改用 Caffeine expireAfterAccess(2min) 自动淘汰——原 ConcurrentHashMap
 * 按 key 只增不减，伪造 XFF 可造成慢速内存泄漏。
 *
 * <p>C4：X-Forwarded-For 仅在请求直接来自可信反向代理时才信任（与 AuthController.clientIp /
 * OperateLogAspect.currentIp 同一 S3 策略）——否则攻击者每请求换一个 XFF 即可绕过限流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    // R7：rate-limit.* 四键 + trusted-proxies 两处 @Value 收敛为 Properties 单点绑定
    private final RateLimitProperties rateLimitProperties;

    /** S3：trusted-proxies 配置来源收敛至 ProxyProperties（与 AuthController/OperateLogAspect 同键单点） */
    private final ProxyProperties proxyProperties;

    private final SysConfigService sysConfigService;

    /** 【P2】rx_config 覆盖值 TTL（毫秒）与键名（系统配置页可维护） */
    static final long CONFIG_TTL_MS = 60_000L;
    static final String KEY_ENABLED = "security.rate-limit.enabled";
    static final String KEY_LOGIN_PER_MINUTE = "security.rate-limit.login-per-minute";
    static final String KEY_COMMAND_PER_MINUTE = "security.rate-limit.command-per-minute";
    static final String KEY_API_PER_MINUTE = "security.rate-limit.api-per-minute";

    /**
     * 【第六章·P2】限流阈值运行时快照：rx_config（security.rate-limit.*）60s 刷新覆盖 yml 缺省，
     * DB 异常沿用 last-known（初始=Properties/yml 默认）。
     */
    record RateLimitRuntime(boolean enabled, int loginPerMinute, int commandPerMinute, int apiPerMinute) {
    }

    private final AtomicReference<RateLimitRuntime> lastKnown = new AtomicReference<>(null);
    private volatile long loadedAtMs;

    private RateLimitRuntime seedFromProperties() {
        return new RateLimitRuntime(rateLimitProperties.isEnabled(),
                rateLimitProperties.getLoginPerMinute(),
                rateLimitProperties.getCommandPerMinute(),
                rateLimitProperties.getApiPerMinute());
    }

    /** 60s TTL 刷新 rx_config 覆盖值；失败沿用 last-known（初始=yml 缺省），避免每请求打库 */
    private RateLimitRuntime current() {
        long now = System.currentTimeMillis();
        RateLimitRuntime rt = lastKnown.get();
        if (rt != null && now - loadedAtMs < CONFIG_TTL_MS) {
            return rt;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            rt = lastKnown.get();
            if (rt != null && now - loadedAtMs < CONFIG_TTL_MS) {
                return rt;
            }
            try {
                RateLimitRuntime fresh = new RateLimitRuntime(
                        Boolean.parseBoolean(sysConfigService.get(KEY_ENABLED,
                                String.valueOf(rt != null ? rt.enabled() : seedFromProperties().enabled()))),
                        intConfig(KEY_LOGIN_PER_MINUTE, rt != null ? rt.loginPerMinute() : seedFromProperties().loginPerMinute()),
                        intConfig(KEY_COMMAND_PER_MINUTE, rt != null ? rt.commandPerMinute() : seedFromProperties().commandPerMinute()),
                        intConfig(KEY_API_PER_MINUTE, rt != null ? rt.apiPerMinute() : seedFromProperties().apiPerMinute()));
                lastKnown.set(fresh);
                loadedAtMs = now;
                return fresh;
            } catch (Exception e) {
                // DB 异常降级：沿用 last-known；从未成功则落 yml 缺省并推迟下次尝试
                RateLimitRuntime fallback = rt != null ? rt : seedFromProperties();
                lastKnown.set(fallback);
                loadedAtMs = now;
                log.warn("[限流] 读取 security.rate-limit.* 失败，沿用上次阈值: {}", e.getMessage());
                return fallback;
            }
        }
    }

    private int intConfig(String key, int defaultValue) {
        try {
            String val = sysConfigService.get(key, String.valueOf(defaultValue));
            return Math.max(1, Integer.parseInt((val != null ? val : String.valueOf(defaultValue)).trim()));
        } catch (NumberFormatException e) {
            log.warn("[限流] 配置 {} 非法整数，使用缺省 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 桶缓存（C4）：key = "ip:pathCategory"，2 分钟无访问自动淘汰，防止无界增长；
     * maximumSize 兜底极端 key 风暴。
     */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!current().enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String category = resolveCategory(path);

        String key = ip + ":" + category;
        Bucket bucket = buckets.get(key, k -> createBucket(category));

        if (bucket != null && bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("[限流] IP {} 触发限流 (category={}, path={})", ip, category, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        }
    }

    /**
     * S3/C4：仅当直连方是可信反向代理时才采信转发头，否则一律取 remoteAddr，
     * 与 AuthController.clientIp、OperateLogAspect.currentIp 保持同一策略。
     */
    private String getClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String trustedProxies = proxyProperties.getTrustedProxies();
        if (remote == null || !StringUtils.hasText(trustedProxies)) {
            return remote;
        }
        boolean trusted = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .anyMatch(p -> "*".equals(p) || p.equalsIgnoreCase(remote));
        if (!trusted) {
            return remote;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remote;
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
        // P2：阈值取运行时快照（rx_config 覆盖 yml 缺省）；已建桶随 2min 淘汰自然切换新阈值
        RateLimitRuntime rt = current();
        switch (category) {
            case "login" -> {
                permits = rt.loginPerMinute();
                period = Duration.ofMinutes(1);
            }
            case "command" -> {
                permits = rt.commandPerMinute();
                period = Duration.ofMinutes(1);
            }
            default -> {
                permits = rt.apiPerMinute();
                period = Duration.ofMinutes(1);
            }
        }
        // B10：配置为 0/负数时 Bucket4j capacity(0) 抛 IllegalArgumentException 导致所有请求 500，钳制下限
        permits = Math.max(1, permits);
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(permits)
                .refillGreedy(permits, period)
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
