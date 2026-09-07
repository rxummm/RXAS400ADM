package com.rxas400adm.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置类型安全绑定（R7：收敛 rxas400.jwt.* 同键多处 @Value 声明）。
 * <p>注册/命名风格对齐 app 模块 {@code AlertUpgradeProperties}（@Component + @ConfigurationProperties）。
 * <p>收敛前分布：JwtUtil(secret/expire-ms/refresh-expire-ms/issuer/audience)、
 * JwtAuthenticationFilter(blacklist-enabled)、WsAuthChannelInterceptor(blacklist-enabled)、
 * TokenBlacklistService(blacklist-enabled/blacklist-fail-closed)、StartupGuard(secret)。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.jwt")
public class JwtProperties {

    /**
     * 签名密钥（生产必须经 RXAS400_JWT_SECRET 环境变量覆盖；StartupGuard 非 mock 下拦截空/默认值）。
     * <p>注意：原 JwtUtil @Value 无默认占位（缺键启动即失败）、原 StartupGuard 兜底空串；
     * yml 固定声明该键（${RXAS400_JWT_SECRET:}），此处 Java 兜底空串与 StartupGuard 原兜底一致，
     * 实际运行行为零变化（R7）。
     */
    private String secret = "";

    /** access token 有效期（毫秒），默认 2h（P2-17：原 24h 偏长） */
    private long expireMs = 7200000L;

    /** P2: Refresh token 有效期（毫秒），默认 7 天 */
    private long refreshExpireMs = 604800000L;

    /** P2-1：令牌签发方 iss 声明 */
    private String issuer = "rxas400adm";

    /** P2-1：令牌目标方 aud 声明 */
    private String audience = "rxas400-ui";

    /** P2-1：是否启用吊销名单检查（默认开启），原三处 @Value 同键收敛 */
    private boolean blacklistEnabled = true;

    /** N4：DB 查询失败时是否按"已吊销"兜底拒绝（默认 true=fail-closed） */
    private boolean blacklistFailClosed = true;
}
