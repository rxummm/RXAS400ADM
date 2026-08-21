package com.rxas400adm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket 实时通道：
 * - 前端订阅 /topic/monitor/{instanceId} 获取实时指标（需 MONITOR_VIEW，见 WsAuthChannelInterceptor）
 * - 前端订阅 /user/queue/notifications 获取个人通知（按用户隔离）
 * 鉴权：CONNECT 必须携带有效 JWT；SUBSCRIBE 目的地按权限码校验（见 WsAuthChannelInterceptor）。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** 与 SecurityConfig 的 CORS 白名单一致（逗号分隔，S4），生产用环境变量覆盖 */
    @Value("${rxas400.security.cors-allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    private final WsAuthChannelInterceptor wsAuthChannelInterceptor;

    public WebSocketConfig(WsAuthChannelInterceptor wsAuthChannelInterceptor) {
        this.wsAuthChannelInterceptor = wsAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // S4：不再通配 "*"，收敛为与 CORS 一致的白名单
                .setAllowedOrigins(Arrays.stream(corsAllowedOrigins.split(","))
                        .map(String::trim)
                        .filter(o -> !o.isBlank())
                        .toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 仅允许服务端发布 /topic（公开频道）与 /queue（个人队列）
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        // 个人队列前缀：convertAndSendToUser → /user/{username}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 帧认证：解析 JWT 写入会话 Principal；SUBSCRIBE 帧目的地鉴权
        registration.interceptors(wsAuthChannelInterceptor);
    }
}
