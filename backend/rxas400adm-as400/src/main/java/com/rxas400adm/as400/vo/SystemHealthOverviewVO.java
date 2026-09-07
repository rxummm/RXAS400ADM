package com.rxas400adm.as400.vo;

import lombok.Data;

/**
 * A1 系统健康概览 VO
 */
@Data
public class SystemHealthOverviewVO {

    /** 总服务器数 */
    private Long totalServers;

    /** 在线服务器数 */
    private Long onlineServers;

    /** 离线服务器数 */
    private Long offlineServers;

    /** 平均 CPU 使用率 */
    private Double avgCpuUsage;

    /** 平均内存使用率 */
    private Double avgMemoryUsage;

    /** 磁盘使用率 */
    private Double avgDiskUsage;

    /** 活跃作业数 */
    private Long activeJobs;

    /** 告警总数 */
    private Long alertCount;

    /** 严重告警数 */
    private Long criticalAlertCount;

    /** 今日备份成功数 */
    private Long todayBackupSuccess;

    /** 今日备份失败数 */
    private Long todayBackupFailed;

    /** 合规通过率（百分比） */
    private Double complianceRate;
}
