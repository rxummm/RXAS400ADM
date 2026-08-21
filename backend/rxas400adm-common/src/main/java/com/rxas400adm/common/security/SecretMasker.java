package com.rxas400adm.common.security;

import java.util.regex.Pattern;

/**
 * 敏感配置掩码（P2-6）：系统参数/Webhook 等含密码、secret 的配置在返回给前端时统一脱敏。
 * - 前端看到占位符 {@link #MASK}；
 * - 提交时若值等于占位符（未改动）→ 服务端保留旧值，避免误覆盖。
 */
public final class SecretMasker {

    private SecretMasker() {
    }

    public static final String MASK = "******";

    private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(password|passwd|pass|secret|token|apikey|api_key).*");

    /** 配置键是否属于敏感项（如 alert.email.pass、alert.webhook.secret） */
    public static boolean isSecretKey(String key) {
        return key != null && SECRET_KEY.matcher(key).matches();
    }

    /** 值是否为掩码占位符（前端未修改原样提交） */
    public static boolean isMasked(String value) {
        return MASK.equals(value);
    }
}
