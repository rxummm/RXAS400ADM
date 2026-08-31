package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.*;
import com.rxas400adm.as400.service.IWmsService;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WMS 仓库管理 REST API。
 */
@RestController
@RequestMapping("/api/v1/bpcs/wms")
@RequiredArgsConstructor
@Tag(name = "WMS 仓库管理")
public class WmsController {

    private final IWmsService wmsService;

    @GetMapping("/warehouses")
    @PreAuthorize("hasAuthority('BPCS_WMS_VIEW')")
    @Operation(summary = "仓库主档查询")
    public ApiResponse<PageResult<BpcsWarehouseVO>> warehouses(BpcsWarehouseQueryDTO dto) {
        return ApiResponse.success(wmsService.searchWarehouses(dto));
    }

    @GetMapping("/bins")
    @PreAuthorize("hasAuthority('BPCS_WMS_VIEW')")
    @Operation(summary = "库位主档查询")
    public ApiResponse<PageResult<BpcsBinVO>> bins(BpcsBinQueryDTO dto) {
        return ApiResponse.success(wmsService.searchBins(dto));
    }

    @GetMapping("/bin-inventory")
    @PreAuthorize("hasAuthority('BPCS_WMS_VIEW')")
    @Operation(summary = "库位库存明细")
    public ApiResponse<PageResult<BpcsBinInventoryVO>> binInventory(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String whse,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.success(wmsService.searchBinInventory(cono, whse, current, size));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('BPCS_WMS_VIEW')")
    @Operation(summary = "库存移动记录")
    public ApiResponse<PageResult<BpcsMovementVO>> movements(BpcsMovementQueryDTO dto) {
        return ApiResponse.success(wmsService.searchMovements(dto));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAuthority('BPCS_WMS_BATCH_VIEW')")
    @Operation(summary = "批次追踪")
    public ApiResponse<PageResult<BpcsBatchTrackingVO>> batches(BpcsBatchQueryDTO dto) {
        return ApiResponse.success(wmsService.searchBatches(dto));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('BPCS_WMS_VIEW')")
    @Operation(summary = "仓库汇总（库位占用率）")
    public ApiResponse<List<BpcsWarehouseSummaryVO>> summary(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(wmsService.warehouseSummary(cono));
    }
}
