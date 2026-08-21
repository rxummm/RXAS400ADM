package com.rxas400adm.security.vo;

import com.rxas400adm.security.entity.LoginAttempt;

import java.time.LocalDateTime;

/**
 * 登录失败/锁定记录视图（P2-10）：与 LoginAttempt 字段一致。
 */
public record LoginAttemptVO(
        String username,
        Long serverId,
        Integer failedCount,
        LocalDateTime lockedUntil,
        LocalDateTime lastFailTime,
        String lastIp,
        LocalDateTime updatedTime) {

    public static LoginAttemptVO from(LoginAttempt e) {
        return new LoginAttemptVO(
                e.getUsername(), e.getServerId(), e.getFailedCount(), e.getLockedUntil(),
                e.getLastFailTime(), e.getLastIp(), e.getUpdatedTime());
    }
}
