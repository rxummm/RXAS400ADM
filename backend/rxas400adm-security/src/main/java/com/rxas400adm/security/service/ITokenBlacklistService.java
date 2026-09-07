package com.rxas400adm.security.service;

public interface ITokenBlacklistService {

    boolean isBlacklisted(String jti);

    /**
     * 原子消费 refresh token（CR-001 修复）：INSERT 成功返回 true，DuplicateKey 返回 false。
     */
    boolean consumeRefreshToken(String jti, String username, long ttlMs);

    void blacklist(String jti, String username, long ttlMs);

    void cleanup();
}