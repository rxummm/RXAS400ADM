package com.rxas400adm.as400.config;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.crypto.AesCryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AES 加密服务配置，从环境变量 RXAS400_CRYPTO_KEY 读取密钥。
 * <p>
 * S4 加固：mock（演示）环境允许回退内置开发密钥；非 mock（prod 等）环境
 * 密钥缺失直接启动失败，避免「漏配环境变量却用公开密钥加密」的静默风险。
 */
@Slf4j
@Configuration
public class CryptoConfig {

    @Value("${rxas400.crypto.key:}")
    private String cryptoKey;

    @Value("${rxas400.crypto.pbkdf2-iterations:120000}")
    private int pbkdf2Iterations;

    private final ProfileResolver profileResolver;

    public CryptoConfig(ProfileResolver profileResolver) {
        this.profileResolver = profileResolver;
    }

    @Bean
    public AesCryptoService aesCryptoService() {
        if (cryptoKey == null || cryptoKey.isBlank()) {
            if (!profileResolver.isMockMode()) {
                throw new IllegalStateException(
                        "RXAS400_CRYPTO_KEY 未配置：非 mock 环境禁止使用内置开发密钥。" +
                                "请设置环境变量 RXAS400_CRYPTO_KEY（生成方式: openssl rand -base64 32）");
            }
            log.warn("================================================");
            log.warn("  RXAS400_CRYPTO_KEY 未配置（mock 环境），密码使用内置开发密钥加密！");
            log.warn("  生产环境请设置环境变量 RXAS400_CRYPTO_KEY");
            log.warn("  生成方式: openssl rand -base64 32");
            log.warn("================================================");
            return new AesCryptoService("RXAS400-DEFAULT-DEV-KEY-PLACEHOLDER", pbkdf2Iterations);
        }
        log.info("AES 加密服务已初始化（密钥来源: 环境变量 RXAS400_CRYPTO_KEY，PBKDF2 迭代 {} 次）", pbkdf2Iterations);
        return new AesCryptoService(cryptoKey, pbkdf2Iterations);
    }
}
