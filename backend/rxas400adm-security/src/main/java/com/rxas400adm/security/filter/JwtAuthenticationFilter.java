package com.rxas400adm.security.filter;

import com.rxas400adm.common.constants.SecurityConstants;
import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.config.ProxyProperties;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.IPermissionService;
import com.rxas400adm.security.service.ITokenBlacklistService;
import jakarta.servlet.FilterChain;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 每次请求校验 Authorization: Bearer <token>，
 * 解析出用户名与权限码写入 SecurityContext。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final IPermissionService permissionService;
    private final ITokenBlacklistService tokenBlacklistService;
    // R7：blacklist-enabled / permission-fallback-on-error 两处 @Value 收敛为 Properties 单点绑定
    private final JwtProperties jwtProperties;
    private final ProxyProperties proxyProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
            if (jwtUtil.isValid(token)) {
                // P2-1：吊销名单检查——登出后的 token 按无权限处理（不设认证），静默放行到匿名链
                if (jwtProperties.isBlacklistEnabled() && tokenBlacklistService.isBlacklisted(jwtUtil.getJti(token))) {
                    log.debug("token 已吊销（jti={}），按匿名处理", jwtUtil.getJti(token));
                    chain.doFilter(request, response);
                    return;
                }
                String username = jwtUtil.getUsername(token);
                // 权限实时从数据库加载（PermissionService 带 60s 缓存），避免 token 签发后
                // 新增权限码（如 REGION_VIEW / CALENDAR_VIEW / ROLE_MANAGE）需重新登录才生效。
                // S1 加固：数据库返回空列表（用户被删除/禁用/无任何权限）**不回退**——空 authorities 使
                // @PreAuthorize 全部拒绝，禁用/删除用户立即失效。
                // P2-5：仅当数据库加载抛异常（如 DB 不可用）且显式开启回退时才使用 token 内嵌权限；
                // 默认拒绝闭合——DB 故障时按无权限处理，避免被禁用用户凭旧 token 继续访问。
                List<String> permissions = List.of();
                try {
                    permissions = permissionService.loadPermissions(username);
                } catch (Exception e) {
                    if (proxyProperties.isPermissionFallbackOnError()) {
                        permissions = jwtUtil.getPermissions(token);
                    } else {
                        log.error("加载用户权限失败(用户名={})，按无权限处理: {}", username, e.getMessage());
                    }
                }
                List<SimpleGrantedAuthority> authorities = permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}