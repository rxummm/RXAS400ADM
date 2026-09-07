package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.service.IBpcsSupplyChainService;
import com.rxas400adm.as400.vo.BpcsAtpVO;
import com.rxas400adm.as400.vo.BpcsAbcAnalysisVO;
import com.rxas400adm.as400.vo.BpcsCrossNodeInventoryVO;
import com.rxas400adm.as400.vo.BpcsDisruptionAlertVO;
import com.rxas400adm.as400.vo.BpcsInventoryAlertVO;
import com.rxas400adm.as400.vo.BpcsInventoryHistoryVO;
import com.rxas400adm.as400.vo.BpcsKpiVO;
import com.rxas400adm.as400.vo.BpcsLoadVO;
import com.rxas400adm.as400.vo.BpcsOtifVO;
import com.rxas400adm.as400.vo.BpcsOrderListVO;
import com.rxas400adm.as400.vo.BpcsOrderTrackingVO;
import com.rxas400adm.as400.vo.BpcsPurchaseReceivingVO;
import com.rxas400adm.as400.vo.BpcsSalesAnalysisVO;
import com.rxas400adm.as400.vo.BpcsSupplierPerfVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BPCS 供应链增强：全部 Phase 1-4 功能。
 */
@RestController
@RequestMapping("/api/v1/bpcs/supply-chain")
@RequiredArgsConstructor
@Tag(name = "BPCS Supply Chain")
public class BpcsSupplyChainController {

    private final IBpcsSupplyChainService sc;

    // ==================== Phase 1 ====================

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public ApiResponse<List<BpcsOrderListVO>> searchOrders(BpcsOrderListQueryDTO query) {
        return ApiResponse.success(sc.searchOrders(query));
    }

    @GetMapping("/inventory/alerts")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    public ApiResponse<List<BpcsInventoryAlertVO>> inventoryAlerts(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(sc.inventoryAlerts(cono, limit));
    }

    @GetMapping("/sales/analysis")
    @PreAuthorize("hasAuthority('BPCS_SALES_VIEW')")
    public ApiResponse<BpcsSalesAnalysisVO> salesAnalysis(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "10") int topN) {
        return ApiResponse.success(sc.salesAnalysis(cono, topN));
    }

    // ==================== Phase 2 ====================

    @GetMapping("/inventory/history")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    public ApiResponse<List<BpcsInventoryHistoryVO>> inventoryHistory(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String item,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(sc.inventoryHistory(cono, item, fromDate, toDate, limit));
    }

    @GetMapping("/purchase/receiving")
    @PreAuthorize("hasAuthority('BPCS_PURCHASE_VIEW')")
    public ApiResponse<List<BpcsPurchaseReceivingVO>> purchaseReceiving(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String pono,
            @RequestParam(required = false) String vendor,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(sc.purchaseReceiving(cono, pono, vendor, limit));
    }

    @GetMapping("/shipping/list")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    public ApiResponse<List<BpcsLoadVO>> shippingList(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String lhno,
            @RequestParam(required = false) String carrier,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(sc.shippingList(cono, lhno, carrier, limit));
    }

    // ==================== Phase 3 ====================

    @GetMapping("/inventory/abc")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    public ApiResponse<List<BpcsAbcAnalysisVO>> abcAnalysis(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(sc.abcAnalysis(cono, limit));
    }

    @GetMapping("/supplier/performance")
    @PreAuthorize("hasAuthority('BPCS_PURCHASE_VIEW')")
    public ApiResponse<List<BpcsSupplierPerfVO>> supplierPerformance(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(sc.supplierPerformance(cono, limit));
    }

    // ==================== Phase 4 ====================

    @GetMapping("/kpi")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public ApiResponse<BpcsKpiVO> kpi(@RequestParam(required = false) String cono) {
        return ApiResponse.success(sc.supplyChainKpi(cono));
    }

    @GetMapping("/order/tracking")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public ApiResponse<BpcsOrderTrackingVO> orderTracking(
            @RequestParam String cono, @RequestParam String orno) {
        return ApiResponse.success(sc.orderTracking(cono, orno));
    }

    // ==================== Phase 5: Control Tower 2.0 ====================

    @GetMapping("/otif")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "OTIF 准时足量交付率追踪")
    public ApiResponse<BpcsOtifVO> otifTracking(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(sc.otifTracking(cono, months));
    }

    @GetMapping("/disruption")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "供应链中断预警")
    public ApiResponse<BpcsDisruptionAlertVO> disruptionAlerts(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(sc.disruptionAlerts(cono, limit));
    }

    @GetMapping("/cross-node")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "跨节点库存可视化")
    public ApiResponse<BpcsCrossNodeInventoryVO> crossNodeInventory(
            @RequestParam(required = false) String cono) {
        return ApiResponse.success(sc.crossNodeInventory(cono));
    }

    // ==================== Phase 6: ATP ====================

    @GetMapping("/atp")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "ATP 可承诺发货（时序+行级承诺）")
    public ApiResponse<BpcsAtpVO> atpOverview(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "8") int weeks) {
        return ApiResponse.success(sc.atpOverview(cono, weeks));
    }

    @GetMapping("/atp/deviation")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "ATP vs OTIF 偏差分析")
    public ApiResponse<List<BpcsAtpVO.AtpDeviation>> atpDeviation(
            @RequestParam(required = false) String cono) {
        return ApiResponse.success(sc.atpDeviation(cono));
    }
}