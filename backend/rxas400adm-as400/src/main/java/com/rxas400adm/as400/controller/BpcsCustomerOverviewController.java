package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsCustomerOverviewService;
import com.rxas400adm.as400.vo.BpcsCustomerOverviewVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户 360° 视图 Controller（⑳）。
 * 权限：BPCS_CUSTOMER_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/customers/overview")
@RequiredArgsConstructor
@Tag(name = "BPCS Customer 360° View")
public class BpcsCustomerOverviewController {

    private final IBpcsCustomerOverviewService overviewService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_CUSTOMER_VIEW')")
    @Operation(summary = "客户 360° 概览")
    public ApiResponse<BpcsCustomerOverviewVO> overview(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String cust,
            @RequestParam(defaultValue = "10") int orderLimit,
            @RequestParam(defaultValue = "10") int invoiceLimit) {
        return ApiResponse.success(overviewService.getOverview(cono, cust, orderLimit, invoiceLimit));
    }
}
