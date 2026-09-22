package com.rxas400adm.as400.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Executive Dashboard 汇总数据（DashboardController.executive 返回）。
 * 扁平结构：直接暴露所有字段，前端无需嵌套解构。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutiveSummaryVO(
        // 系统概览
        Integer totalServers,
        Integer onlineServers,
        Integer offlineServers,
        // 监控指标（取所有服务器平均值）
        Long avgCpu,
        Long avgMemory,
        Long avgDisk,
        Long activeAlerts,
        Long criticalAlerts,
        // BPCS 业务概览
        Integer totalOrders,
        Integer closedOrders,
        BigDecimal completionRate,
        Integer totalItems,
        BigDecimal inventoryValue,
        BigDecimal onTimeDeliveryRate,
        // 销售趋势（最近 6 个月）
        java.util.List<MonthDataVO> salesTrend,
        // 热销产品 TOP5
        java.util.List<TopEntryVO> topItems,
        // 大客户 TOP5
        java.util.List<TopEntryVO> topCustomers
) {
    /** 月度数据点 */
    public record MonthDataVO(String ym, BigDecimal revenue, Integer orderCount) {}

    /** Top N 条目 */
    public record TopEntryVO(String code, String name, Integer count, BigDecimal amount) {}
}
