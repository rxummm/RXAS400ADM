package com.rxas400adm.as400;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.as400.context.As400ServerContextHolder;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.crypto.AesCryptoService;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多服务器客户端管理：
 * - mock 模式：每服务器一个 MockAS400Client（带服务器名标识）
 * - 生产模式：每服务器一个 JTOpenAS400Client，按服务器 ID 缓存
 * <p>
 * S2 加固：服务器 update/delete/连接测试后调用 {@link #evict(Long)}，
 * 清除客户端连接缓存与配置缓存（60s），改主机/账号/密码后无需重启即生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AS400ClientProviderImpl implements AS400ClientProvider {

    private final IbmiSystemMapper systemMapper;
    private final AesCryptoService aesCryptoService;
    private final ConcurrentMap<Long, AS400Client> clientCache = new ConcurrentHashMap<>();

    /** 服务器配置缓存（60s），避免 forServer 每次请求打库（P3） */
    private final Cache<Long, IbmiSystem> systemCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(500)
            .build();

    private final ProfileResolver profileResolver;

    @Override
    public AS400Client current() {
        Long serverId = As400ServerContextHolder.getServerId();
        if (serverId != null) {
            return forServer(serverId);
        }
        // C10：非请求线程（@Async/Quartz/采集线程）调用 current() 时 holder 为空——
        // 静默回落默认服务器会造成「错服路由且无报错」，至少留痕 warn 供排查
        log.warn("current() 在无 X-AS400-Server 上下文的线程中调用，回落默认服务器（如非预期请改用 forServer(id)）");
        return defaultClient();
    }

    @Override
    public AS400Client forServer(Long serverId) {
        IbmiSystem system = loadSystem(serverId);
        if (Boolean.FALSE.equals(system.getEnabled())) {
            throw new BusinessException(ErrorCode.AS400_SERVER_DISABLED, "Server is disabled: " + system.getName());
        }
        // mock 与生产都按服务器缓存：保证客户端内状态（如子系统启停、计数器）跨请求一致
        return clientCache.computeIfAbsent(serverId, id -> {
            if (profileResolver.isMockMode()) {
                return new MockAS400Client(system.getName());
            }
            return new JTOpenAS400Client(system.getHost(), system.getUsername(), decryptPassword(system));
        });
    }

    @Override
    public void evict(Long serverId) {
        if (serverId == null) {
            return;
        }
        // P1：先释放 JT400 底层连接再移除缓存，避免半开连接残留
        AS400Client client = clientCache.remove(serverId);
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                log.warn("AS400 客户端连接释放失败: serverId={}, {}", serverId, e.getMessage());
            }
        }
        systemCache.invalidate(serverId);
        log.info("AS400 服务器客户端/配置缓存已失效: serverId={}", serverId);
    }

    private AS400Client defaultClient() {
        IbmiSystem system = systemMapper.selectOne(new LambdaQueryWrapper<IbmiSystem>()
                .eq(IbmiSystem::getEnabled, true)
                .eq(IbmiSystem::getDefaultServer, true)
                .last(PageConstants.limitClause(1)));
        if (system == null) {
            // 无默认服务器时取第一个启用的
            system = systemMapper.selectOne(new LambdaQueryWrapper<IbmiSystem>()
                    .eq(IbmiSystem::getEnabled, true)
                    .orderByAsc(IbmiSystem::getSortOrder)
                    .last(PageConstants.limitClause(1)));
        }
        if (system == null) {
            throw new BusinessException(ErrorCode.AS400_SERVER_NOT_FOUND, "No available AS400 server configured");
        }
        return forServer(system.getId());
    }

    private IbmiSystem loadSystem(Long serverId) {
        IbmiSystem system = systemCache.getIfPresent(serverId);
        if (system != null) {
            return system;
        }
        system = systemMapper.selectById(serverId);
        if (system == null) {
            throw new BusinessException(ErrorCode.AS400_SERVER_NOT_FOUND, "Server not found: " + serverId);
        }
        systemCache.put(serverId, system);
        return system;
    }

    private String decryptPassword(IbmiSystem system) {
        return aesCryptoService.decrypt(system.getPasswordEncrypt());
    }
}
