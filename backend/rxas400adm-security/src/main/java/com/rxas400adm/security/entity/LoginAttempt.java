package com.rxas400adm.security.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录失败记录与账号锁定（rx_login_attempt，2.2.7 增强）：数据库持久化，重启不清零；
 * 按 用户名+服务器 维度统计（server_id=0 表示平台登录）。
 *
 * 复合主键（username, server_id）不适用 @TableId(type = IdType.AUTO)，
 * 所有查询使用 LambdaQueryWrapper / LambdaUpdateWrapper，从不使用 xxxById 方法，
 * 因此无需 @TableId 注解。此为 CODING_STANDARDS.md §2.3.1 的合规例外。
 */
@Data
@TableName("rx_login_attempt")
public class LoginAttempt {

    private String username;

    /** 0=平台登录；>0=AS400 服务器 ID */
    private Long serverId;

    private Integer failedCount;

    /** 锁定到期时间，NULL=未锁定 */
    private LocalDateTime lockedUntil;

    private LocalDateTime lastFailTime;

    private String lastIp;

    private LocalDateTime updatedTime;
}