package com.rxas400adm.config;

import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.common.constants.SecurityConstants;
import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.config.ProxyProperties;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.IPermissionService;
import com.rxas400adm.security.service.ITokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
@Component
@RequiredArgsConstructor
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final IPermissionService permissionService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final IbmiSystemMapper systemMapper;
    // R7：blacklist-enabled / permission-fallback-on-error 两处 @Value 收敛为 Properties 单点绑定
    private final JwtProperties jwtProperties;
    private final ProxyProperties proxyProperties;

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
            throw new AccessDeniedException("WebSocket connection requires a valid login token");
        }
        // N1：与 JwtAuthenticationFilter 一致——已吊销的 token（登出后）不再放行 WS 连接，
        // 否则旧 token 在有效期内仍可订阅 /topic/monitor/** 实时数据
        if (jwtProperties.isBlacklistEnabled() && tokenBlacklistService.isBlacklisted(jwtUtil.getJti(token))) {
            log.warn("[WS] CONNECT token 已吊销(jti={})，拒绝连接", jwtUtil.getJti(token));
            throw new AccessDeniedException("Login token has been revoked, please log in again");
        }
        String username = jwtUtil.getUsername(token);
        // 与 JwtAuthenticationFilter 一致（P2-5）：权限实时从数据库加载，默认拒绝闭合——
        // 仅当 DB 故障且显式开启 rxas400.security.permission-fallback-on-error 时才回退 token 声明
        List<String> permissions;
        try {
            permissions = permissionService.loadPermissions(username);
        } catch (Exception e) {
            if (proxyProperties.isPermissionFallbackOnError()) {
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
            throw new AccessDeniedException("Subscription requires authentication");
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Missing subscription destination");
        }
        if (destination.startsWith("/topic/monitor/")) {
            boolean hasMonitorView = authentication.getAuthorities().stream()
                    .anyMatch(a -> "MONITOR_VIEW".equals(a.getAuthority()));
            if (!hasMonitorView) {
                log.warn("[WS] 用户 {} 无 MONITOR_VIEW，拒绝订阅 {}", authentication.getName(), destination);
                throw new AccessDeniedException("No permission to access real-time monitoring data");
            }
            // B7：实例级校验——destination 尾部必须是存在且启用的服务器 ID，
            // 防止任意 MONITOR_VIEW 用户订阅所有服务器（含未授权实例）的实时指标
            String tail = destination.substring("/topic/monitor/".length());
            Long instanceId;
            try {
                instanceId = Long.parseLong(tail);
            } catch (NumberFormatException e) {
                log.warn("[WS] 用户 {} 订阅目的地格式非法: {}", authentication.getName(), destination);
                throw new AccessDeniedException("Invalid monitoring subscription destination");
            }
            IbmiSystem system = systemMapper.selectById(instanceId);
            if (system == null || Boolean.FALSE.equals(system.getEnabled())) {
                log.warn("[WS] 用户 {} 订阅的服务器不存在或已禁用: id={}", authentication.getName(), instanceId);
                throw new AccessDeniedException("Monitoring server not found or disabled");
            }
        } else if (destination.startsWith("/topic/")) {
            // 当前服务端仅向 /topic/monitor/** 发布；其余公开 topic 一律拒绝
            log.warn("[WS] 用户 {} 订阅未授权目的地 {}", authentication.getName(), destination);
            throw new AccessDeniedException("No permission to subscribe to this destination");
        }
        // /user/queue/** 与 /app/** 仅需已登录
    }
}