package com.rxas400adm.common.constants;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** JWT 请求头 */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 放行路径（无需登录） */
    public static final String[] PERMIT_ALL = {
            "/api/v1/auth/login",
            "/api/v1/auth/as400-login",
            "/api/v1/as400/servers/enabled",
            "/ws/**",
            "/error",
            "/actuator/health"
    };

    /**
     * Swagger/OpenAPI 路径：不纳入 PERMIT_ALL 免登录白名单。
     * 仅 dev/mock/test profile 由 SecurityConfig 按环境放行（防御纵深，见 SecurityConfig.REQUIRE_AUTH_SWAGGER）；
     * 生产依赖 springdoc.api-docs.enabled=false + 未放行双重保障。
     */
    public static final String[] SWAGGER_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

}
