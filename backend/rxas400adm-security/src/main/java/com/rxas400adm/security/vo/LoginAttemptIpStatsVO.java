package com.rxas400adm.security.vo;

import java.util.Map;

/**
 * 登录尝试 IP 统计（AuthController.loginAttemptIps 返回）。
 * SQL 查询 rx_login_attempt 聚合结果。
 */
public record LoginAttemptIpStatsVO(Map<String, Object> data) {
    public static LoginAttemptIpStatsVO from(Map<String, Object> map) {
        return new LoginAttemptIpStatsVO(map);
    }
}
