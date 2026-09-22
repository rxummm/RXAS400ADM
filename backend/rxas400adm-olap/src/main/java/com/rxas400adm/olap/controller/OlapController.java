package com.rxas400adm.olap.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.olap.service.OlapAnalysisService;
import com.rxas400adm.olap.vo.OlapInventorySummaryVO;
import com.rxas400adm.olap.vo.OlapPurchaseSummaryVO;
import com.rxas400adm.olap.vo.OlapSalesSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/olap")
@RequiredArgsConstructor
@Tag(name = "OLAP分析", description = "多维数据分析查询")
public class OlapController {

    private final OlapAnalysisService service;

    @GetMapping("/sales-summary")
    @PreAuthorize("hasAuthority('OLAP_SALES_VIEW')")
    @Operation(summary = "销售汇总分析")
    public ApiResponse<PageResult<OlapSalesSummaryVO>> salesSummary(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.salesSummary(cono, period, current, size));
    }

    @GetMapping("/inventory-summary")
    @PreAuthorize("hasAuthority('OLAP_INVENTORY_VIEW')")
    @Operation(summary = "库存汇总分析")
    public ApiResponse<PageResult<OlapInventorySummaryVO>> inventorySummary(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String warehouse,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.inventorySummary(cono, warehouse, current, size));
    }

    @GetMapping("/purchase-summary")
    @PreAuthorize("hasAuthority('OLAP_PURCHASE_VIEW')")
    @Operation(summary = "采购汇总分析")
    public ApiResponse<PageResult<OlapPurchaseSummaryVO>> purchaseSummary(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String vendorCode,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.purchaseSummary(cono, vendorCode, current, size));
    }
}