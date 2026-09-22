package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsTmsLiteService;
import com.rxas400adm.as400.vo.BpcsTmsLiteVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * TMS Lite 运输管理 Controller。
 * 路线规划、承运商比价、运费分析、签收追踪。
 */
@RestController
@RequestMapping("/api/v1/bpcs/tms")
@RequiredArgsConstructor
@Tag(name = "BPCS TMS Lite")
public class BpcsTmsLiteController {

    private final IBpcsTmsLiteService tmsService;

    @GetMapping("/route-plans")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    @Operation(summary = "路线规划列表")
    public ApiResponse<PageResult<BpcsTmsLiteVO.RoutePlan>> getRoutePlans(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(tmsService.getRoutePlans(cono, current, size));
    }

    @GetMapping("/carrier-comparison")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    @Operation(summary = "承运商比价")
    public ApiResponse<PageResult<BpcsTmsLiteVO.CarrierComparison>> getCarrierComparison(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(tmsService.getCarrierComparison(cono, current, size));
    }

    @GetMapping("/freight-analysis")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    @Operation(summary = "运费分析")
    public ApiResponse<BpcsTmsLiteVO.FreightAnalysis> getFreightAnalysis(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(tmsService.getFreightAnalysis(cono, months));
    }

    @GetMapping("/delivery-tracking")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    @Operation(summary = "签收追踪")
    public ApiResponse<PageResult<BpcsTmsLiteVO.DeliveryTracking>> getDeliveryTracking(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(tmsService.getDeliveryTracking(cono, status, current, size));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    @Operation(summary = "TMS 综合数据")
    public ApiResponse<BpcsTmsLiteVO> getAll(
            @RequestParam(required = false) String cono,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(tmsService.getAll(cono, months));
    }
}