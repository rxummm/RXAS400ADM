package com.rxas400adm.common.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 统一的 profile 判定（H4/B10 收敛点）。
 * <p>
 * mock / dev / test 视为非生产（松安全、AS400 走 Mock 客户端、JWT 跳过强校验）；
 * 其余（含未配置 profile）一律按 prod 处理（fail-secure）。
 * 取代原先散落在 6 个类中的 {@code isMockMode()} / {@code activeProfile.contains("mock")} 重复实现。
 */
@Component
public class ProfileResolver {

    private final Environment environment;

    public ProfileResolver(Environment environment) {
        this.environment = environment;
    }

    /** dev/mock/test 均视为非生产（Swagger 免登录、异常回显细节） */
    public boolean isDevLikeMode() {
        return environment.acceptsProfiles(Profiles.of("mock", "dev", "test"));
    }

    /** 仅 mock 模式（AS400 走 Mock 客户端、跳过每日同步、允许默认加密密钥） */
    public boolean isMockMode() {
        return environment.acceptsProfiles(Profiles.of("mock"));
    }
}
