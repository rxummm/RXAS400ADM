package com.rxas400adm.security.vo;

/**
 * 登录尝试 IP 聚合统计（AuthController.loginAttemptIps 返回）。
 */
public record LoginAttemptIpVO(String ip, long attempts, String lastAttempt, String usernames) {
}
