package com.rxas400adm.as400.vo;

/**
 * 执行统计 VO。
 */
public record ExecutionStatsVO(
        long totalExecutions,
        long successCount,
        long failedCount,
        double successRate,
        double avgCostMs
) {
}
