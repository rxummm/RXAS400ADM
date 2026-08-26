package com.rxas400adm.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CORS 白名单配置类型安全绑定（R7）。
 * <p>注册/命名风格对齐 app 模块 {@code AlertUpgradeProperties}。
 * <p>说明（R7 归属标注）：SecurityConfig 与 WebSocketConfig 引用同一 yml 键
 * rxas400.security.cors-allowed-origins（实际前缀 rxas400.security，非任务预设的 rxas400.cors——
 * 键名禁止改动，prefix 从实），两处同键声明收敛至此单点。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rxas400.security")
public class CorsProperties {

    /** S4：CORS 允许来源白名单（逗号分隔），默认开发源 localhost:5173，生产用环境变量覆盖 */
    private String corsAllowedOrigins = "http://localhost:5173";
}
