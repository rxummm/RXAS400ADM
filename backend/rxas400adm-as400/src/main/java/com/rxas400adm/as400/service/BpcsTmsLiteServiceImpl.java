package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsTmsLiteVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TMS Lite 运输管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsTmsLiteServiceImpl implements IBpcsTmsLiteService {

    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsTmsLiteVO.RoutePlan> getRoutePlans(String cono, int limit) {
        if (profileResolver.isMockMode()) {
            return mockRoutePlans();
        }
        return mockRoutePlans();
    }

    @Override
    public List<BpcsTmsLiteVO.CarrierComparison> getCarrierComparison(String cono) {
        if (profileResolver.isMockMode()) {
            return mockCarrierComparison();
        }
        return mockCarrierComparison();
    }

    @Override
    public BpcsTmsLiteVO.FreightAnalysis getFreightAnalysis(String cono, int months) {
        if (profileResolver.isMockMode()) {
            return mockFreightAnalysis();
        }
        return mockFreightAnalysis();
    }

    @Override
    public List<BpcsTmsLiteVO.DeliveryTracking> getDeliveryTracking(String cono, String status, int limit) {
        if (profileResolver.isMockMode()) {
            return mockDeliveryTracking();
        }
        return mockDeliveryTracking();
    }

    @Override
    public BpcsTmsLiteVO getAll(String cono, int months) {
        return new BpcsTmsLiteVO(
                getRoutePlans(cono, 50),
                getCarrierComparison(cono),
                getFreightAnalysis(cono, months),
                getDeliveryTracking(cono, null, 50));
    }

    // ==================== Mock 数据 ====================

    private List<BpcsTmsLiteVO.RoutePlan> mockRoutePlans() {
        List<BpcsTmsLiteVO.RoutePlan> routes = new ArrayList<>();
        routes.add(new BpcsTmsLiteVO.RoutePlan(
                "RT-001", "上海", "苏州", 120.5, 2.5, 2, "顺丰速运",
                new BigDecimal("1580.00"), "COMPLETED", List.of("SO-20250701", "SO-20250703")));
        routes.add(new BpcsTmsLiteVO.RoutePlan(
                "RT-002", "上海", "常州", 180.3, 3.0, 1, "德邦物流",
                new BigDecimal("2340.00"), "IN_TRANSIT", List.of("SO-20250710")));
        routes.add(new BpcsTmsLiteVO.RoutePlan(
                "RT-003", "上海", "杭州", 170.8, 2.8, 3, "中通快递",
                new BigDecimal("1200.00"), "PLANNED", List.of("SO-20250715", "SO-20250716", "SO-20250718")));
        routes.add(new BpcsTmsLiteVO.RoutePlan(
                "RT-004", "上海", "南京", 300.0, 4.5, 1, "京东物流",
                new BigDecimal("3200.00"), "PLANNED", List.of("SO-20250720")));
        return routes;
    }

    private List<BpcsTmsLiteVO.CarrierComparison> mockCarrierComparison() {
        List<BpcsTmsLiteVO.CarrierComparison> carriers = new ArrayList<>();
        carriers.add(new BpcsTmsLiteVO.CarrierComparison(
                "SF", "顺丰速运", 4.8, 156, 95.2, new BigDecimal("8.50"), new BigDecimal("1.5"),
                "标准", true));
        carriers.add(new BpcsTmsLiteVO.CarrierComparison(
                "DB", "德邦物流", 4.3, 98, 88.7, new BigDecimal("6.20"), new BigDecimal("2.0"),
                "标准", false));
        carriers.add(new BpcsTmsLiteVO.CarrierComparison(
                "ZT", "中通快递", 3.9, 210, 82.1, new BigDecimal("4.80"), new BigDecimal("2.5"),
                "经济", false));
        carriers.add(new BpcsTmsLiteVO.CarrierComparison(
                "JD", "京东物流", 4.5, 67, 92.5, new BigDecimal("9.80"), new BigDecimal("1.2"),
                "特快", false));
        return carriers;
    }

    private BpcsTmsLiteVO.FreightAnalysis mockFreightAnalysis() {
        List<BpcsTmsLiteVO.CarrierCostShare> shares = List.of(
                new BpcsTmsLiteVO.CarrierCostShare("顺丰速运", new BigDecimal("132600"), 35.2, 156),
                new BpcsTmsLiteVO.CarrierCostShare("德邦物流", new BigDecimal("60760"), 16.1, 98),
                new BpcsTmsLiteVO.CarrierCostShare("中通快递", new BigDecimal("100800"), 26.8, 210),
                new BpcsTmsLiteVO.CarrierCostShare("京东物流", new BigDecimal("65660"), 17.4, 67),
                new BpcsTmsLiteVO.CarrierCostShare("其他", new BigDecimal("16000"), 4.5, 24));
        List<BpcsTmsLiteVO.MonthlyFreightTrend> trends = List.of(
                new BpcsTmsLiteVO.MonthlyFreightTrend("202601", new BigDecimal("58000"), 62, new BigDecimal("935")),
                new BpcsTmsLiteVO.MonthlyFreightTrend("202602", new BigDecimal("52000"), 55, new BigDecimal("945")),
                new BpcsTmsLiteVO.MonthlyFreightTrend("202603", new BigDecimal("65000"), 70, new BigDecimal("928")),
                new BpcsTmsLiteVO.MonthlyFreightTrend("202604", new BigDecimal("62000"), 68, new BigDecimal("912")),
                new BpcsTmsLiteVO.MonthlyFreightTrend("202605", new BigDecimal("68000"), 72, new BigDecimal("944")),
                new BpcsTmsLiteVO.MonthlyFreightTrend("202606", new BigDecimal("70220"), 68, new BigDecimal("1033")));
        List<BpcsTmsLiteVO.FreightCostByRoute> byRoutes = List.of(
                new BpcsTmsLiteVO.FreightCostByRoute("上海→苏州", new BigDecimal("1580"), 45, 15.5),
                new BpcsTmsLiteVO.FreightCostByRoute("上海→常州", new BigDecimal("2340"), 32, 20.3),
                new BpcsTmsLiteVO.FreightCostByRoute("上海→杭州", new BigDecimal("1200"), 58, 18.2),
                new BpcsTmsLiteVO.FreightCostByRoute("上海→南京", new BigDecimal("3200"), 18, 19.8),
                new BpcsTmsLiteVO.FreightCostByRoute("其他", new BigDecimal("800"), 88, 26.2));
        return new BpcsTmsLiteVO.FreightAnalysis(
                new BigDecimal("375820"), new BigDecimal("954"), new BigDecimal("7.12"),
                5.8, shares, trends, byRoutes);
    }

    private List<BpcsTmsLiteVO.DeliveryTracking> mockDeliveryTracking() {
        List<BpcsTmsLiteVO.DeliveryTracking> trackings = new ArrayList<>();
        trackings.add(new BpcsTmsLiteVO.DeliveryTracking(
                "L-20250701", "SO-20250701", "顺丰速运", "上海", "苏州", "2026-08-25", "2026-08-26",
                "2026-08-26", "DELIVERED", "已签收", "张工",
                "POD-001", List.of(
                new BpcsTmsLiteVO.TrackingEvent("2026-08-25 14:00", "上海", "已揽收", "包裹已从分拣中心发出"),
                new BpcsTmsLiteVO.TrackingEvent("2026-08-25 22:00", "中转站", "运输中", "到达苏州中转站"),
                new BpcsTmsLiteVO.TrackingEvent("2026-08-26 08:00", "苏州", "派送中", "快递员李师傅开始派送"),
                new BpcsTmsLiteVO.TrackingEvent("2026-08-26 11:30", "苏州", "已签收", "张工签收"))));
        trackings.add(new BpcsTmsLiteVO.DeliveryTracking(
                "L-20250615", "SO-20250710", "德邦物流", "上海", "常州", "2026-08-28", "2026-08-31",
                null, "IN_TRANSIT", "运输中", null, null, List.of(
                new BpcsTmsLiteVO.TrackingEvent("2026-08-28 10:00", "上海", "已揽收", "货物已装车出发"),
                new BpcsTmsLiteVO.TrackingEvent("2026-08-29 06:00", "中转站", "运输中", "货物在途"))));
        trackings.add(new BpcsTmsLiteVO.DeliveryTracking(
                "L-20250601", "SO-20250715", "中通快递", "上海", "杭州", "2026-08-30", "2026-09-01",
                null, "PENDING", "待发货", null, null, List.of()));
        trackings.add(new BpcsTmsLiteVO.DeliveryTracking(
                "L-20250520", "SO-20250716", "京东物流", "上海", "南京", "2026-08-20", "2026-08-22",
                "2026-08-21", "DELIVERED", "已签收", "王仓管",
                "POD-002", List.of(
                new BpcsTmsLiteVO.TrackingEvent("2026-08-20 09:00", "上海", "已揽收", "包裹已发出"),
                new BpcsTmsLiteVO.TrackingEvent("2026-08-21 15:00", "南京", "已签收", "王仓管签收"))));
        return trackings;
    }
}
