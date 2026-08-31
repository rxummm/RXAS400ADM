package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsInventoryConsistencyQueryDTO;
import com.rxas400adm.as400.service.IBpcsInventoryAnalyticsService;
import com.rxas400adm.as400.vo.BpcsInventoryConsistencyVO;
import com.rxas400adm.as400.vo.BpcsInventorySlowMovingVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存分析 Controller（多级一致性核对、呆滞物料）。
 * 权限：BPCS_INVENTORY_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/inventory/analytics")
@RequiredArgsConstructor
@Tag(name = "BPCS库存分析")
public class BpcsInventoryAnalyticsController {

    private final IBpcsInventoryAnalyticsService analyticsService;

    @GetMapping("/consistency")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "库存多级一致性核对")
    public ApiResponse<BpcsInventoryConsistencyVO> checkConsistency(
            @Valid BpcsInventoryConsistencyQueryDTO query) {
        return ApiResponse.success(analyticsService.checkConsistency(query.getCono(), query.getItem()));
    }

    @GetMapping("/slow-moving")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "呆滞物料分析")
    public ApiResponse<List<BpcsInventorySlowMovingVO>> slowMoving(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String cutoffDate,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(analyticsService.getSlowMovingItems(cono, cutoffDate, limit));
    }
}
