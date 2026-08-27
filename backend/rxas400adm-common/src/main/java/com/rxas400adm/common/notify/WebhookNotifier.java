package com.rxas400adm.common.notify;

import com.rxas400adm.common.security.SsrfGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Webhook 告警推送（2.1.6 增强）：POST JSON 到外部 URL，\n * 最多尝试 {@link #MAX_ATTEMPTS} 次（间隔 {@link #RETRY_DELAY}），失败记录日志。\n * URL 由调用方从 sys_config（alert.webhook.url）解析；空 URL 调用方直接跳过。\n */
@Slf4j
@Component
public class WebhookNotifier {

    static final int MAX_ATTEMPTS = 3;
    static final Duration RETRY_DELAY = Duration.ofMillis(500);
    static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient = RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory())
            .build();

    /** L2：URL 安全校验器（默认 SSRF 守卫；构造器注入供测试绕开环回限制验证 HTTP 通路） */
    private final Predicate<String> urlValidator;

    public WebhookNotifier() {
        this(SsrfGuard::isSafeUrl);
    }

    WebhookNotifier(Predicate<String> urlValidator) {
        this.urlValidator = urlValidator;
    }

    /**
     * 推送一条告警，返回是否成功（失败重试后仍失败返回 false，不抛异常）。
     */
    public boolean push(String url, String title, String message) {
        return pushDetailed(url, title, message).success();
    }

    /** 推送结果：是否成功 + 尝试次数 + 最终错误信息（供发送日志落库） */
    public record PushResult(boolean success, int attempts, String errorMsg) {
    }

    /** 推送并返回详细结果（供 rx_webhook_log 落库） */
    public PushResult pushDetailed(String url, String title, String message) {
        if (url == null || url.isBlank()) {
            return new PushResult(false, 0, "URL 为空");
        }
        // L2：SSRF 防护——出站出口统一校验，禁私网/回环/链路本地目标
        if (!urlValidator.test(url)) {
            log.warn("[Webhook] 推送地址不合法（SSRF 防护拒绝）: {}", url);
            return new PushResult(false, 0, "URL 不在允许范围（仅公网 http/https）");
        }
        Map<String, Object> payload = Map.of(
                "title", title,
                "message", message,
                "timestamp", LocalDateTime.now().toString());
        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return new PushResult(true, attempt, null);
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("[Webhook] 推送失败(第 {}/{} 次, url={}): {}", attempt, MAX_ATTEMPTS, url, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis() * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new PushResult(false, attempt, lastError);
                    }
                }
            }
        }
        log.error("[Webhook] 推送最终失败: {}", url);
        return new PushResult(false, MAX_ATTEMPTS, lastError);
    }
}
