package com.rxas400adm.system.service;

import com.rxas400adm.common.security.ClBlacklistConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 【第六章·P1】CL 黑名单运行时配置提供者——从 rx_config 读取，供 common 校验器消费。
 *
 * <p>键（系统配置页可维护，实时生效）：
 * <ul>
 *   <li>{@code cl.blacklist.enabled}：true/false，缺省 true；</li>
 *   <li>{@code cl.blacklist.extra-verbs}：逗号分隔追加动词，缺省空。</li>
 * </ul>
 *
 * <p>降级策略：读取/解析失败时沿用上一次成功值（last-known-good），
 * 从未成功过则回退安全默认（enabled=true、无扩展）——宁可沿用旧黑名单也不放行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RxConfigClBlacklistProvider implements ClBlacklistConfigProvider {

    public static final String KEY_ENABLED = "cl.blacklist.enabled";
    public static final String KEY_EXTRA_VERBS = "cl.blacklist.extra-verbs";

    private final SysConfigService sysConfigService;

    /** last-known-good 快照：初始为安全默认 */
    private final AtomicReference<Boolean> lastEnabled = new AtomicReference<>(Boolean.TRUE);
    private final AtomicReference<List<String>> lastExtraVerbs =
            new AtomicReference<>(List.of());

    @Override
    public boolean enabled() {
        refresh();
        return Boolean.TRUE.equals(lastEnabled.get());
    }

    @Override
    public List<String> extraVerbs() {
        refresh();
        return lastExtraVerbs.get();
    }

    /**
     * 每次调用都尝试刷新（common 校验器自带 60s TTL 节流，此处不再重复缓存）；
     * 单键异常不中断另一键的读取。
     */
    private void refresh() {
        try {
            String raw = sysConfigService.get(KEY_ENABLED, "true");
            lastEnabled.set(!"false".equalsIgnoreCase(raw == null ? "true" : raw.trim()));
        } catch (Exception e) {
            log.warn("读取 {} 失败，沿用上次值: {}", KEY_ENABLED, e.getMessage());
        }
        try {
            String raw = sysConfigService.get(KEY_EXTRA_VERBS, "");
            lastExtraVerbs.set(parseVerbs(raw));
        } catch (Exception e) {
            log.warn("读取 {} 失败，沿用上次值: {}", KEY_EXTRA_VERBS, e.getMessage());
        }
    }

    /** 解析追加动词：逗号分隔 → 大写去空白去空项（与原 yml 解析语义一致） */
    private List<String> parseVerbs(String config) {
        List<String> result = new ArrayList<>();
        if (config == null || config.isBlank()) {
            return result;
        }
        for (String part : config.split(",")) {
            String verb = part.trim().toUpperCase(Locale.ROOT);
            if (!verb.isEmpty()) {
                result.add(verb);
            }
        }
        return result;
    }
}
