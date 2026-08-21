package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.security.entity.LoginAttempt;
import com.rxas400adm.security.mapper.LoginAttemptMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录安全（追踪文档 2.2.7，已持久化增强）：\n * <ul>\n *   <li><b>失败锁定（数据库持久化）</b>：同一 用户名+服务器 连续失败 {@link #MAX_FAILED} 次后\n *       锁定 {@link #LOCK_DURATION} 分钟，重启不清零；锁定到期自动解锁并清零计数。</li>\n *   <li><b>IP 限流（Caffeine）</b>：同一 IP 每分钟最多 {@code maxIpPerMinute}（默认 20，可配置）次登录尝试\n
 *       （1 分钟窗口属瞬态，保持内存态）。</li>\n * </ul>\n */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService implements ILoginAttemptService {

    /** 平台登录的 server_id 占位 */
    static final long PLATFORM_SERVER = 0L;

    /** P2-4：失败锁定阈值与时长可配置（rxas400.security.login.*），默认 5 次/15 分钟 */
    @Value("${rxas400.security.login.max-failed:5}")
    private int maxFailed = 5;

    @Value("${rxas400.security.login.lock-minutes:15}")
    private long lockMinutes = 15;

    /** P3-4：IP 每分钟登录尝试上限可配置（rxas400.security.login.max-ip-per-minute），默认 20 */
    @Value("${rxas400.security.login.max-ip-per-minute:20}")
    private int maxIpPerMinute = 20;

    static final Duration IP_WINDOW = Duration.ofMinutes(1);

    private final LoginAttemptMapper attemptMapper;

    private final Cache<String, AtomicInteger> ipCache = Caffeine.newBuilder()
            .expireAfterWrite(IP_WINDOW)
            .maximumSize(10000)
            .build();

    /** 检查用户名是否被锁定（serverId 为空视为平台登录） */
    public void checkUsernameLock(String username, Long serverId) {
        LoginAttempt attempt = find(username, serverId);
        if (attempt == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (attempt.getLockedUntil() != null && attempt.getLockedUntil().isAfter(now)) {
            long minutes = Duration.between(now, attempt.getLockedUntil()).toMinutes() + 1;
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, "账号已锁定，请 " + minutes + " 分钟后再试");
        }
        if (attempt.getLockedUntil() != null) {
            // 锁定已过期：自动解锁并清零
            clearFailure(username, serverId);
        }
    }

    /** 记录一次登录失败（原子累计；达到阈值置锁定到期时间） */
    public void registerFailure(String username, Long serverId, String ip) {
        Long server = normalize(serverId);
        LoginAttempt existing = find(username, server);
        if (existing == null) {
            try {
                LoginAttempt attempt = new LoginAttempt();
                attempt.setUsername(username);
                attempt.setServerId(server);
                attempt.setFailedCount(1);
                attempt.setLastFailTime(LocalDateTime.now());
                attempt.setLastIp(ip);
                attempt.setUpdatedTime(LocalDateTime.now());
                attemptMapper.insert(attempt);
                return;
            } catch (DuplicateKeyException e) {
                // P2-4：并发首失败时另一请求已插入，落入下方增量路径
                log.warn("登录失败记录并发插入冲突(用户名={})，走增量路径", username);
            }
        }
        attemptMapper.incrementFailure(username, server, ip);
        LoginAttempt updated = find(username, server);
        if (updated != null && updated.getFailedCount() != null
                && updated.getFailedCount() >= maxFailed && updated.getLockedUntil() == null) {
            LoginAttempt lock = new LoginAttempt();
            lock.setLockedUntil(LocalDateTime.now().plus(Duration.ofMinutes(lockMinutes)));
            attemptMapper.update(lock, new LambdaUpdateWrapper<LoginAttempt>()
                    .eq(LoginAttempt::getUsername, username)
                    .eq(LoginAttempt::getServerId, server)
                    .isNull(LoginAttempt::getLockedUntil));
        }
    }

    /** 登录成功清除失败记录 */
    public void clearFailure(String username, Long serverId) {
        attemptMapper.delete(new LambdaQueryWrapper<LoginAttempt>()
                .eq(LoginAttempt::getUsername, username)
                .eq(LoginAttempt::getServerId, normalize(serverId)));
    }

    /** 登录失败记录列表（按服务器维度统计；serverId 为空返回全部） */
    public List<LoginAttempt> listAttempts(Long serverId) {
        LambdaQueryWrapper<LoginAttempt> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(LoginAttempt::getServerId, normalize(serverId));
        }
        wrapper.orderByDesc(LoginAttempt::getLastFailTime);
        return attemptMapper.selectList(wrapper);
    }

    /** 按 IP 聚合统计（暴力破解溯源） */
    public List<Map<String, Object>> aggregateByIp() {
        return attemptMapper.aggregateByIp();
    }

    /** 检查并累计 IP 登录频率（超限抛异常） */
    public void checkIpRate(String ip) {
        AtomicInteger counter = ipCache.get(ip, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > maxIpPerMinute) {
            throw new BusinessException(ErrorCode.LOGIN_TOO_MANY, "登录尝试过于频繁，请稍后再试");
        }
    }

    private LoginAttempt find(String username, Long serverId) {
        return attemptMapper.selectOne(new LambdaQueryWrapper<LoginAttempt>()
                .eq(LoginAttempt::getUsername, username)
                .eq(LoginAttempt::getServerId, normalize(serverId)));
    }

    private Long normalize(Long serverId) {
        return serverId == null ? PLATFORM_SERVER : serverId;
    }
}