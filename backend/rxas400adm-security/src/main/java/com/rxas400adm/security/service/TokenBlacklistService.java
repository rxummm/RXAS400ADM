package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.entity.TokenBlacklist;
import com.rxas400adm.security.mapper.TokenBlacklistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * JWT 吊销名单（P2-1）：登出/下线时按 jti 登记，有效期内拒绝认证。
 * 存储于 DB（rx_token_blacklist），多节点部署天然一致；过期记录每小时清理一次。
 * N4：查询走 30s 本地缓存（Caffeine）+ DB 异常兜底，避免每请求打库、DB 抖动导致全站认证失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService implements ITokenBlacklistService {

    private final TokenBlacklistMapper blacklistMapper;

    // R7：blacklist-enabled / blacklist-fail-closed 两处 @Value 收敛为 JwtProperties 单点绑定
    // （N4：与 JwtAuthenticationFilter/WsAuthChannelInterceptor 共用配置）
    private final JwtProperties jwtProperties;

    /** N4：查询结果本地缓存（30s），DB 打点频率从每请求降到每 30s/每 jti 一次 */
    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10_000)
            .build();

    /** 判断 jti 是否已吊销 */
    public boolean isBlacklisted(String jti) {
        if (!jwtProperties.isBlacklistEnabled() || jti == null || jti.isBlank()) {
            return false;
        }
        Boolean cached = cache.getIfPresent(jti);
        if (cached != null) {
            return cached;
        }
        boolean blacklisted;
        try {
            blacklisted = blacklistMapper.selectCount(new LambdaQueryWrapper<TokenBlacklist>()
                    .eq(TokenBlacklist::getJti, jti)) > 0;
        } catch (Exception e) {
            // N4：DB 不可用/抖动——fail-closed 拒绝（宁可误拒不放行已吊销 token），避免空窗期
            log.warn("[JWT] 吊销名单查询失败(jti={})，按{}处理: {}", jti,
                    jwtProperties.isBlacklistFailClosed() ? "已吊销拒绝" : "未吊销放行", e.getMessage());
            return jwtProperties.isBlacklistFailClosed();
        }
        cache.put(jti, blacklisted);
        return blacklisted;
    }

    /**
     * 原子消费 refresh token（rotation 防竞态）：INSERT 成功 → 返回 true（允许签发新 token）；
     * DuplicateKey → 返回 false（已被并发消费，必须拒绝）。
     * 解决 CR-001：check→blacklist 之间的 TOCTOU 竞态窗口。
     */
    public boolean consumeRefreshToken(String jti, String username, long ttlMs) {
        if (jti == null || jti.isBlank() || ttlMs <= 0) {
            return false;
        }
        try {
            TokenBlacklist row = new TokenBlacklist();
            row.setJti(jti);
            row.setUsername(username == null ? "" : username);
            row.setExpireTime(LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(ttlMs)));
            row.setCreatedTime(LocalDateTime.now());
            blacklistMapper.insert(row);
            cache.put(jti, true);
            return true;
        } catch (DuplicateKeyException e) {
            // 已被并发消费，拒绝
            cache.put(jti, true);
            return false;
        } catch (Exception e) {
            log.error("[JWT] 吊销登记失败(jti={})，token 仍有效: {}", jti, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Token blacklisting failed: " + e.getMessage());
        }
    }

    /** 登记吊销；ttlMs<=0 或 jti 为空忽略；并发重复插入走唯一索引静默跳过 */
    public void blacklist(String jti, String username, long ttlMs) {
        if (jti == null || jti.isBlank() || ttlMs <= 0) {
            return;
        }
        try {
            TokenBlacklist row = new TokenBlacklist();
            row.setJti(jti);
            row.setUsername(username == null ? "" : username);
            row.setExpireTime(LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(ttlMs)));
            row.setCreatedTime(LocalDateTime.now());
            blacklistMapper.insert(row);
            cache.put(jti, true);
        } catch (DuplicateKeyException e) {
            // 已吊销，忽略
            cache.put(jti, true);
        } catch (Exception e) {
            log.error("[JWT] 吊销登记失败(jti={})，token 仍有效: {}", jti, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Token blacklisting failed: " + e.getMessage());
        }
    }

    /** 定期清理过期记录，避免表无限增长 */
    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        try {
            int removed = blacklistMapper.delete(new LambdaQueryWrapper<TokenBlacklist>()
                    .lt(TokenBlacklist::getExpireTime, LocalDateTime.now()));
            if (removed > 0) {
                log.info("[JWT] 清理过期吊销记录 {} 条", removed);
            }
        } catch (Exception e) {
            log.warn("[JWT] 吊销记录清理失败: {}", e.getMessage());
        }
    }
}