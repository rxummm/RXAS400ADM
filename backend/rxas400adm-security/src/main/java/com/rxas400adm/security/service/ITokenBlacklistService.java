package com.rxas400adm.security.service;

public interface ITokenBlacklistService {

    boolean isBlacklisted(String jti);

    void blacklist(String jti, String username, long ttlMs);

    void cleanup();
}