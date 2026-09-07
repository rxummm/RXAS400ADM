package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.vo.BpcsAbcAnalysisVO;
import com.rxas400adm.as400.vo.BpcsInventoryAlertVO;
import com.rxas400adm.as400.vo.BpcsInventoryHistoryVO;
import com.rxas400adm.as400.vo.BpcsKpiVO;
import com.rxas400adm.as400.vo.BpcsLoadVO;
import com.rxas400adm.as400.vo.BpcsOrderListVO;
import com.rxas400adm.as400.vo.BpcsOrderTrackingVO;
import com.rxas400adm.as400.vo.BpcsPurchaseReceivingVO;
import com.rxas400adm.as400.vo.BpcsSalesAnalysisVO;
import com.rxas400adm.as400.vo.BpcsCrossNodeInventoryVO;
import com.rxas400adm.as400.vo.BpcsDisruptionAlertVO;
import com.rxas400adm.as400.vo.BpcsAtpVO;
import com.rxas400adm.as400.vo.BpcsOtifVO;
import com.rxas400adm.as400.vo.BpcsSupplierPerfVO;

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

    // ==================== Phase 5: Control Tower 2.0 ====================

    /** OTIF 准时足量交付率追踪 */
    BpcsOtifVO otifTracking(String cono, int months);

    /** 供应链中断预警 */
    BpcsDisruptionAlertVO disruptionAlerts(String cono, int limit);

    /** 跨节点库存可视化 */
    BpcsCrossNodeInventoryVO crossNodeInventory(String cono);

    // ==================== Phase 6: ATP ====================

    /** ATP 汇总 + 时序 + 订单行承诺 */
    BpcsAtpVO atpOverview(String cono, int weeks);

    /** ATP-OTIF 偏差分析 */
    List<BpcsAtpVO.AtpDeviation> atpDeviation(String cono);
}