package com.rxas400adm.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.constants.SecurityConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** 当前 profile 判定（统一收敛到 ProfileResolver）：Swagger 仅在 dev/mock/test 免登录 */
    private final ProfileResolver profileResolver;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    // R7：cors-allowed-origins（SecurityConfig 与 WebSocketConfig 同键）收敛为 CorsProperties 单点绑定
    private final CorsProperties corsProperties;

    public SecurityConfig(ProfileResolver profileResolver,
                          @Lazy JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper,
                          CorsProperties corsProperties) {
        this.profileResolver = profileResolver;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.corsProperties = corsProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PERMIT_ALL).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(swaggerMatchers()).permitAll()
                        // P2: Actuator 基础端点（health/info）公开；metrics 需认证
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        // S6：CSP 纵深防御——script-src 仅 'self'，配合前端禁 v-html 双保险；
                        // connect-src 放行 WebSocket（监控/通知实时推送）；style-src 放行内联样式（ECharts/Element 运行时设置）
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; font-src 'self'; connect-src 'self' ws: wss:; "
                                        + "frame-ancestors 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error(401, "Not logged in or token expired")));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error(403, "Access denied")));
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // S4：不再通配 "*"，收敛为配置化白名单（allowCredentials=true 时不允许 *）
        List<String> origins = Arrays.stream(corsProperties.getCorsAllowedOrigins().split(","))
                .map(String::trim)
                .filter(o -> !o.isBlank())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "X-AS400-Server",
                "Accept", "Origin", "Cache-Control", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Swagger 放行矩阵：仅 dev/mock/test 环境免登录，其余环境返回空数组（一律走认证） */
    private String[] swaggerMatchers() {
        return profileResolver.isDevLikeMode() ? SecurityConstants.SWAGGER_PATHS : new String[0];
    }
}
