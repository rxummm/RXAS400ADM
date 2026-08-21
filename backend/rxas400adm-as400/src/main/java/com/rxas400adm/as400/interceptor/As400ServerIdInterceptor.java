package com.rxas400adm.as400.interceptor;

import com.rxas400adm.as400.context.As400ServerContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从请求 Header "X-AS400-Server"（或参数 serverId）提取目标服务器 ID 存入 ThreadLocal。
 * 前端在请求拦截器中注入当前选择的服务器。
 */
@Slf4j
@Component
public class As400ServerIdInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-AS400-Server";
    public static final String PARAM_NAME = "serverId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String id = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(id)) {
            id = request.getParameter(PARAM_NAME);
        }
        if (StringUtils.hasText(id)) {
            try {
                As400ServerContextHolder.setServerId(Long.valueOf(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("无效的 AS400 Server ID: {}", id);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        As400ServerContextHolder.clear();
    }
}
