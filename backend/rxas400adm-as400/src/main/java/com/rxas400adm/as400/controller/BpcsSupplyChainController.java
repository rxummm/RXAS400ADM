package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.service.IBpcsSupplyChainService;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.response.ApiResponse;
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
@Tag(name = "BPCS供应链")
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
}
