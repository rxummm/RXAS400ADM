package com.rxas400adm.config;

import com.rxas400adm.system.entity.SysConfig;
import com.rxas400adm.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动安全校验：
 * <ol>
 *   <li>mock 模式启动时打印醒目警告</li>
 *   <li>非 mock 模式下 JWT 密钥为默认值时拒绝启动</li>
 *   <li>数据库配置 system.mock-mode-enabled=false 时拒绝 mock 模式启动</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupGuard {

    private final SysConfigMapper configMapper;

    @Value("${spring.profiles.active:mock}")
    private String activeProfile;

    /** P3-6：实际生效的 JWT 密钥（含 yml 内置默认值兜底），非 mock 时与已知默认值比对拦截 */
    @Value("${rxas400.jwt.secret:}")
    private String jwtSecret;

    /** 已知内置默认密钥（application.yml 兜底值）——生产用此值即视为未配置 */
    private static final String KNOWN_DEFAULT_SECRET = "RXAS400-Enterprise-IBM-i-Operation-Platform-Secret-2026";

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        boolean isMock = activeProfile.contains("mock");

        if (isMock) {
            log.warn("================================================");
            log.warn("  当前运行在 MOCK 模式，所有 AS400 数据为模拟数据");
            log.warn("  请勿用于生产环境！");
            log.warn("  生产部署请设置 spring.profiles.active=prod");
            log.warn("================================================");
        }

        // P3-6：非 mock 模式校验 JWT 密钥——未设置环境变量（落入 yml 默认值）
        // 或显式配置了内置默认值字符串时都拒绝启动，防止使用可被猜出的签名密钥。
        if (!isMock && (System.getenv("RXAS400_JWT_SECRET") == null
                || KNOWN_DEFAULT_SECRET.equals(jwtSecret))) {
            log.error("================================================");
            log.error("  JWT 密钥未配置或仍为内置默认值，存在安全风险！");
            log.error("  请设置环境变量 RXAS400_JWT_SECRET（>=32 字节随机值）");
            log.error("  应用将拒绝启动");
            log.error("================================================");
            throw new IllegalStateException(
                    "JWT secret is not set or still using the built-in default. "
                            + "Please set RXAS400_JWT_SECRET environment variable.");
        }

        if (isMock) {
            try {
                SysConfig config = configMapper.selectById("system.mock-mode-enabled");
                if (config != null && "false".equalsIgnoreCase(config.getConfigValue().trim())) {
                    log.error("================================================");
                    log.error("  数据库配置 system.mock-mode-enabled=false");
                    log.error("  禁止 mock 模式启动，请设置 spring.profiles.active=prod");
                    log.error("  或删除该配置项以恢复 mock 模式");
                    log.error("  应用将拒绝启动");
                    log.error("================================================");
                    throw new IllegalStateException(
                            "Mock mode is disabled by system config 'system.mock-mode-enabled=false'. "
                                    + "Set spring.profiles.active=prod or remove this config.");
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.debug("数据库配置检查跳过（表可能尚未初始化）: {}", e.getMessage());
            }
        }
    }
}