package com.rxas400adm.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 请求限流配置类型安全绑定（R7：收敛 rxas400.security.rate-limit.* 四处 @Value 到单点）。
 * <p>注册/命名风格对齐 app 模块 {@code AlertUpgradeProperties}。
 * <p>消费方：RateLimitFilter（Bucket4j 限流阈值与总开关）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.security.rate-limit")
public class RateLimitProperties {

    /** 限流总开关，默认开启 */
    private boolean enabled = true;

    /** 登录端点限流（次/分钟），默认 5（防暴力破解） */
    private int loginPerMinute = 5;

    /** 命令执行端点限流（次/分钟），默认 10（防滥用） */
    private int commandPerMinute = 10;

    /** 通用 API 限流（次/分钟），默认 60（防洪泛） */
    private int apiPerMinute = 60;
}
