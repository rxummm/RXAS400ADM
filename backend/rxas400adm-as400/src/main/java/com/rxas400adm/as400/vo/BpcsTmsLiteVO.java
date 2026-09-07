package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * TMS Lite 运输管理 VO。
 * 路线规划、承运商比价、运费分析、签收追踪。
 */
public record BpcsTmsLiteVO(
        List<RoutePlan> routePlans,
        List<CarrierComparison> carrierComparisons,
        FreightAnalysis freightAnalysis,
        List<DeliveryTracking> deliveryTrackings
) {
    /** 路线规划 */
    public record RoutePlan(
            String routeId,
            String origin,
            String destination,
            double distanceKm,
            double estimatedHours,
            int stopCount,
            String carrier,
            BigDecimal estimatedCost,
            String status,
            List<String> orderNos
    ) {}

    /** 承运商比价 */
    public record CarrierComparison(
            String carrierCode,
            String carrierName,
            double rating,
            int totalShipments,
            double onTimeRate,
            BigDecimal avgCostPerKg,
            BigDecimal avgTransitDays,
            String serviceLevel,
            boolean recommended
    ) {}

    /** 运费分析汇总 */
    public record FreightAnalysis(
            BigDecimal totalFreightCost,
            BigDecimal avgCostPerShipment,
            BigDecimal avgCostPerKg,
            double costChangePct,
            List<CarrierCostShare> carrierShares,
            List<MonthlyFreightTrend> monthlyTrends,
            List<FreightCostByRoute> costByRoutes
    ) {}

    /** 承运商费用占比 */
    public record CarrierCostShare(
            String carrier,
            BigDecimal totalCost,
            double sharePct,
            int shipmentCount
    ) {}

    /** 月度运费趋势 */
    public record MonthlyFreightTrend(
            String ym,
            BigDecimal totalCost,
            int shipmentCount,
            BigDecimal avgCost
    ) {}

    /** 按路线的运费分布 */
    public record FreightCostByRoute(
            String route,
            BigDecimal avgCost,
            int count,
            double pctOfTotal
    ) {}

    /** 签收追踪 */
    public record DeliveryTracking(
            String loadNo,
            String orderNo,
            String carrier,
            String origin,
            String destination,
            String shipDate,
            String estimatedArrival,
            String actualArrival,
            String status,
            String statusKey,
            String signedBy,
            String proofOfDelivery,
            List<TrackingEvent> events
    ) {}

    /** 物流追踪事件 */
    public record TrackingEvent(
            String timestamp,
            String location,
            String event,
            String detail
    ) {}
}
