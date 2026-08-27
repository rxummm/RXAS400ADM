package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.vo.*;

import java.util.List;

/**
 * BPCS 供应链增强服务：订单列表搜索 / 库存预警 / 销售分析 / Phase 2-4 功能。
 */
public interface IBpcsSupplyChainService {

    /** 订单列表搜索 */
    List<BpcsOrderListVO> searchOrders(BpcsOrderListQueryDTO query);

    /** 库存预警（可用量 < 安全库存） */
    List<BpcsInventoryAlertVO> inventoryAlerts(String cono, int limit);

    /** 销售分析（Top N） */
    BpcsSalesAnalysisVO salesAnalysis(String cono, int topN);

    /** Phase 2: 库存变动历史 */
    List<BpcsInventoryHistoryVO> inventoryHistory(String cono, String item, String fromDate, String toDate, int limit);

    /** Phase 2: 采购收货管理 */
    List<BpcsPurchaseReceivingVO> purchaseReceiving(String cono, String pono, String vendor, int limit);

    /** Phase 2: 发运列表视图 */
    List<BpcsLoadVO> shippingList(String cono, String lhno, String carrier, int limit);

    /** Phase 3: ABC 分析 */
    List<BpcsAbcAnalysisVO> abcAnalysis(String cono, int limit);

    /** Phase 3: 供应商绩效 */
    List<BpcsSupplierPerfVO> supplierPerformance(String cono, int limit);

    /** Phase 4: 供应链 KPI */
    BpcsKpiVO supplyChainKpi(String cono);

    /** Phase 4: 订单全链路追踪 */
    BpcsOrderTrackingVO orderTracking(String cono, String orno);
}
