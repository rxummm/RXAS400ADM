package com.rxas400adm.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全配置类型安全绑定（R7：收敛 rxas400.security.login.* 三处 @Value 到单点）。
 * <p>注册/命名风格对齐 app 模块 {@code AlertUpgradeProperties}。
 * <p>消费方：LoginAttemptService（失败锁定阈值/时长、IP 每分钟登录尝试上限）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.security.login")
public class LoginSecurityProperties {

    /** P2-4：连续失败锁定阈值（次），默认 5 次 */
    private int maxFailed = 5;

    /** P2-4：锁定时长（分钟），默认 15 分钟 */
    private long lockMinutes = 15L;

    /** P3-4：同一 IP 每分钟登录尝试上限，默认 20 */
    private int maxIpPerMinute = 20;
}
