package com.rxas400adm.as400.config;

import com.rxas400adm.as400.context.As400ServerContextHolder;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * P1-7：聚合查询缓存键生成器——同一方法对不同 AS400 服务器返回不同数据，
 * 缓存键必须包含当前请求的服务器 ID（As400ServerContextHolder.ThreadLocal），
 * 否则多服务器环境下会跨服务器串数据。
 * <p>
 * M3：无上下文（serverId 为 null，如定时任务/异步线程）时拼接随机键，
 * 使每次调用键唯一——宁可缓存不命中，也不允许跨服务器数据互相污染。
 */
@Component("serverAwareKeyGenerator")
public class ServerAwareKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();
        Long serverId = As400ServerContextHolder.getServerId();
        if (serverId == null) {
            // M3：空上下文生成一次性随机键，避免「null 上下文」调用共享同一缓存键串数据
            key.append("nctx").append('|').append(UUID.randomUUID());
        } else {
            key.append(serverId);
        }
        key.append('|').append(method.getName());
        for (Object p : params) {
            key.append('|');
            if (p != null) {
                key.append(p);
            }
        }
        return key.toString();
    }
}