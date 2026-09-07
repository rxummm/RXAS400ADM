package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsOrderDetailService;
import com.rxas400adm.as400.vo.BpcsOrderLineDetailVO;
import com.rxas400adm.as400.vo.BpcsOrderTimelineEventVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单详情增强 Controller（⑰）。
 * 权限：BPCS_ORDER_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orders/detail")
@RequiredArgsConstructor
@Tag(name = "BPCS Order Detail Enhanced")
public class BpcsOrderDetailController {

    private final IBpcsOrderDetailService detailService;

    @GetMapping("/lines")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "订单行增强详情（含发运和发票）")
    public ApiResponse<List<BpcsOrderLineDetailVO>> lineDetails(
            @RequestParam String cono, @RequestParam String orno) {
        return ApiResponse.success(detailService.getLineDetails(cono, orno));
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    @Operation(summary = "订单历史事件时间线")
    public ApiResponse<List<BpcsOrderTimelineEventVO>> timeline(
            @RequestParam String cono, @RequestParam String orno) {
        return ApiResponse.success(detailService.getTimeline(cono, orno));
    }
}
