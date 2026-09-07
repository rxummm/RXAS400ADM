package com.rxas400adm.as400.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A2 安全审计摘要 VO
 */
@Data
public class SecurityAuditSummaryVO {

    /** 服务器ID */
    private Long serverId;

    /** 服务器名称 */
    private String serverName;

    /** 总登录次数 */
    private Long totalLogins;

    /** 成功登录次数 */
    private Long successfulLogins;

    /** 失败登录次数 */
    private Long failedLogins;

    /** 独立用户数 */
    private Long uniqueUsers;

    /** 独立IP数 */
    private Long uniqueIps;

    /** 权限变更次数 */
    private Long permissionChanges;

    /** 高危操作次数 */
    private Long highRiskOperations;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最近高危操作时间 */
    private LocalDateTime lastHighRiskTime;
}
