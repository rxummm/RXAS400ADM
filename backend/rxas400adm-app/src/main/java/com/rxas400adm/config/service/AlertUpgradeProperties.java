package com.rxas400adm.config.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 告警升级（E1）类型安全配置绑定。
 * <p>项目首个 {@link ConfigurationProperties} 样板；后续 rxas400.security.* / rxas400.jwt.*
 * 等散布 @Value 按此模式分域收敛（见 docs/项目增强和实现-2026-08-24.md 问题 C2-5）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.alert.upgrade")
public class AlertUpgradeProperties {

    /** 功能开关，默认关闭 */
    private boolean enabled = false;

    /** 定时检查间隔（毫秒），默认 5 分钟 */
    private long checkIntervalMs = 300_000L;

    /** 未处理多久后升级（分钟） */
    private int upgradeAfterMinutes = 30;

    /** 升级通知的目标角色 */
    private String notifyRole = "ADMIN";
}
