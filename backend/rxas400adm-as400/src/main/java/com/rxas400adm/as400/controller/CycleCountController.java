package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.CycleCountPlanDTO;
import com.rxas400adm.as400.dto.CycleCountResultDTO;
import com.rxas400adm.as400.service.ICycleCountService;
import com.rxas400adm.as400.vo.CycleCountPlanVO;
import com.rxas400adm.as400.vo.CycleCountResultVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 循环盘点 Controller（㉙）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/cycle-count")
@RequiredArgsConstructor
@Tag(name = "BPCS Cycle Count")
public class CycleCountController {

    private final ICycleCountService cycleCountService;

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @OperateLog(module = OperateLogModule.BPCS_INVENTORY, operation = OperateLogOperation.CREATE_COUNT_PLAN)
    @Operation(summary = "创建盘点计划")
    public ApiResponse<CycleCountPlanVO> createPlan(
            @Valid @RequestBody CycleCountPlanDTO dto) {
        return ApiResponse.success(cycleCountService.createPlan(dto, SecurityUtils.currentUsername()));
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "获取盘点计划列表")
    public ApiResponse<PageResult<CycleCountPlanVO>> listPlans(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(cycleCountService.listPlans(status, current, size));
    }

    @PostMapping("/results")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @OperateLog(module = OperateLogModule.BPCS_INVENTORY, operation = OperateLogOperation.RECORD_COUNT_RESULT)
    @Operation(summary = "录入盘点结果")
    public ApiResponse<CycleCountResultVO> recordResult(
            @Valid @RequestBody CycleCountResultDTO dto,
            @RequestParam int systemQty) {
        return ApiResponse.success(cycleCountService.recordResult(dto, SecurityUtils.currentUsername(), systemQty));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "获取盘点结果列表")
    public ApiResponse<PageResult<CycleCountResultVO>> listResults(
            @RequestParam Long planId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(cycleCountService.listResults(planId, current, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "盘点汇总统计")
    public ApiResponse<Map<String, Object>> summary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) cycleCountService.getSummary(fromDate, toDate);
        return ApiResponse.success(result);
    }
}