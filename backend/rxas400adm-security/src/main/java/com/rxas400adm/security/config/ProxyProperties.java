package com.rxas400adm.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * rxas400.security 前缀散键绑定（R7）：可信反向代理白名单 + 权限回退开关。
 * <p>注册/命名风格对齐 app 模块 {@code AlertUpgradeProperties}。
 * <p>说明（R7 归属标注）：
 * <ul>
 *   <li>trusted-proxies：原三处 @Value 同键声明收敛至此
 *       （AuthController.clientIp / OperateLogAspect.currentIp / RateLimitFilter.getClientIp）；
 *       OperateLogAspect 因 system 模块不依赖 security 模块（避免循环依赖）暂留 @Value，
 *       键归属以本类为单点。</li>
 *   <li>permission-fallback-on-error：实际 yml 键前缀是 rxas400.security（非 rxas400.jwt），
 *       故归入本类而非 JwtProperties——保证绑定键名不变、行为零变化；
 *       原 JwtAuthenticationFilter / WsAuthChannelInterceptor 两处同键声明收敛至此。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.security")
public class ProxyProperties {

    /**
     * 可信反向代理 IP 列表（逗号分隔，S3）；留空则完全忽略 X-Forwarded-For / X-Real-IP。
     * 判定逻辑三处复制维持不动，本次仅收敛配置来源（抽公共工具列为后续）。
     */
    private String trustedProxies = "";

    /**
     * P2-5：数据库加载权限抛异常时是否回退 token 内嵌声明（默认 false=拒绝闭合）。
     * 开启后 DB 故障窗口内被禁用/删除用户可能凭旧 token 继续访问（可用性优先，需自行权衡）。
     */
    private boolean permissionFallbackOnError = false;
}
