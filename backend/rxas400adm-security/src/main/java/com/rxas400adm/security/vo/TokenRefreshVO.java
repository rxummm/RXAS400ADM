package com.rxas400adm.security.vo;

/**
 * Token 刷新响应 VO（替代 Map&lt;String, Object&gt;）。
 */
public record TokenRefreshVO(String token, String refreshToken, long expireMs) {}
