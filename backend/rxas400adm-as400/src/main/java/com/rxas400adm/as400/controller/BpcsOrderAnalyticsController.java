package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsOrderFulfillmentQueryDTO;
import com.rxas400adm.as400.service.IBpcsOrderAnalyticsService;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订单分析 Controller（履行率、OTD、Backorder）。
 * 权限：BPCS_ORDER_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orders/analytics")
@RequiredArgsConstructor
@Tag(name = "BPCS Order Analytics")
public class BpcsOrderAnalyticsController {

    private final IBpcsOrderAnalyticsService analyticsService;

    @GetMapping("/fulfillment")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "订单履行率统计")
    public ApiResponse<BpcsOrderFulfillmentStatsVO> fulfillmentStats(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(analyticsService.getFulfillmentStats(cono));
    }

    @GetMapping("/backorder")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "Backorder 行明细")
    public ApiResponse<PageResult<BpcsOrderBackorderLineVO>> backorderLines(
            @Valid BpcsOrderFulfillmentQueryDTO query) {
        return ApiResponse.success(analyticsService.getBackorderLines(query));
    }

    @GetMapping("/backorder/by-item")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "Backorder 按物料聚合")
    public ApiResponse<PageResult<BpcsOrderBackorderByItemVO>> backorderByItem(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(analyticsService.getBackorderByItem(cono, current, size));
    }

    @GetMapping("/otd")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "交期绩效 OTD 统计")
    public ApiResponse<BpcsOrderOtdStatsVO> otdStats(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(analyticsService.getOtdStats(cono));
    }

    @GetMapping("/otd/by-customer")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "OTD 按客户聚合")
    public ApiResponse<PageResult<BpcsOrderOtdByCustomerVO>> otdByCustomer(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(analyticsService.getOtdByCustomer(cono, current, size));
    }
}