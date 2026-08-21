package com.rxas400adm.config;

import com.rxas400adm.common.constants.SecurityConstants;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.IPermissionService;
import com.rxas400adm.security.service.ITokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * WebSocket STOMP 会话认证与订阅鉴权（P0-3 加固）：
 * - CONNECT：必须携带有效 JWT（Authorization: Bearer &lt;token&gt;），无效/缺失直接拒绝连接
 *   （不再放行匿名）；权限与 JwtAuthenticationFilter 一致——实时从数据库加载，DB 故障才回退
 *   token 内嵌声明。
 * - SUBSCRIBE：按目的地鉴权——
 *   · /topic/monitor/** → 需要 MONITOR_VIEW（实时监控数据不外泄）
 *   · /topic/** 其它目的地 → 拒绝（当前服务端仅发布 /topic/monitor/**）
 *   · /user/queue/**（个人通知）→ 已登录即可
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final IPermissionService permissionService;
    private final ITokenBlacklistService tokenBlacklistService;

    /** P2-1：WS 链路是否启用 JWT 吊销名单检查（与 JwtAuthenticationFilter 一致，默认开启） */
    @org.springframework.beans.factory.annotation.Value("${rxas400.jwt.blacklist-enabled:true}")
    private boolean blacklistEnabled;

    /** P2-5：与 JwtAuthenticationFilter 一致——DB 故障时是否回退 token 内嵌权限（默认 false=拒绝闭合） */
    @org.springframework.beans.factory.annotation.Value("${rxas400.security.permission-fallback-on-error:false}")
    private boolean permissionFallbackOnError;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            checkSubscribe(accessor);
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader(SecurityConstants.HEADER_AUTHORIZATION);
        String token = null;
        if (auth != null && StringUtils.hasText(auth)) {
            token = auth.startsWith(SecurityConstants.TOKEN_PREFIX)
                    ? auth.substring(SecurityConstants.TOKEN_PREFIX.length()).trim()
                    : auth.trim();
        }
        if (token == null || !jwtUtil.isValid(token)) {
            log.warn("[WS] CONNECT 缺少有效 JWT，拒绝连接");
            throw new AccessDeniedException("WebSocket 连接需要有效登录令牌");
        }
        // N1：与 JwtAuthenticationFilter 一致——已吊销的 token（登出后）不再放行 WS 连接，
        // 否则旧 token 在有效期内仍可订阅 /topic/monitor/** 实时数据
        if (blacklistEnabled && tokenBlacklistService.isBlacklisted(jwtUtil.getJti(token))) {
            log.warn("[WS] CONNECT token 已吊销(jti={})，拒绝连接", jwtUtil.getJti(token));
            throw new AccessDeniedException("登录令牌已失效，请重新登录");
        }
        String username = jwtUtil.getUsername(token);
        // 与 JwtAuthenticationFilter 一致（P2-5）：权限实时从数据库加载，默认拒绝闭合——
        // 仅当 DB 故障且显式开启 rxas400.security.permission-fallback-on-error 时才回退 token 声明
        List<String> permissions;
        try {
            permissions = permissionService.loadPermissions(username);
        } catch (Exception e) {
            if (permissionFallbackOnError) {
                permissions = jwtUtil.getPermissions(token);
            } else {
                log.error("[WS] 加载用户权限失败(用户名={})，按无权限处理: {}", username, e.getMessage());
                permissions = List.of();
            }
        }
        List<GrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, authorities));
        accessor.setLeaveMutable(true);
    }

    private void checkSubscribe(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated() || authentication.getName() == null) {
            log.warn("[WS] 匿名 SUBSCRIBE 被拒绝: {}", accessor.getDestination());
            throw new AccessDeniedException("订阅需要登录");
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("缺少订阅目的地");
        }
        if (destination.startsWith("/topic/monitor/")) {
            boolean hasMonitorView = authentication.getAuthorities().stream()
                    .anyMatch(a -> "MONITOR_VIEW".equals(a.getAuthority()));
            if (!hasMonitorView) {
                log.warn("[WS] 用户 {} 无 MONITOR_VIEW，拒绝订阅 {}", authentication.getName(), destination);
                throw new AccessDeniedException("无权限订阅实时监控数据");
            }
        } else if (destination.startsWith("/topic/")) {
            // 当前服务端仅向 /topic/monitor/** 发布；其余公开 topic 一律拒绝
            log.warn("[WS] 用户 {} 订阅未授权目的地 {}", authentication.getName(), destination);
            throw new AccessDeniedException("无权限订阅该目的地");
        }
        // /user/queue/** 与 /app/** 仅需已登录
    }
}