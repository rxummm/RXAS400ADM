package com.rxas400adm.as400.context;

/**
 * AS400 服务器上下文持有者（借鉴旧项目设计）。
 * ThreadLocal 存储当前请求对应的服务器 ID，
 * 由 As400ServerIdInterceptor 从 Header / 请求参数注入，
 * 供 AS400ClientProvider 和业务 Service 使用。
 */
public final class As400ServerContextHolder {

    private static final ThreadLocal<Long> SERVER_ID_HOLDER = new ThreadLocal<>();

    private As400ServerContextHolder() {
    }

    public static void setServerId(Long serverId) {
        SERVER_ID_HOLDER.set(serverId);
    }

    public static Long getServerId() {
        return SERVER_ID_HOLDER.get();
    }

    public static void clear() {
        SERVER_ID_HOLDER.remove();
    }
}
