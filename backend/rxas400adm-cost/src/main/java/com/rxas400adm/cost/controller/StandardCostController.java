package com.rxas400adm.cost.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.cost.service.StandardCostService;
import com.rxas400adm.cost.vo.StandardCostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cost/standard-costs")
@RequiredArgsConstructor
@Tag(name = "标准成本", description = "标准成本数据管理")
public class StandardCostController {

    private final StandardCostService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COST_STANDARD_VIEW')")
    @Operation(summary = "分页查询标准成本")
    public ApiResponse<PageResult<StandardCostVO>> page(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String costComponent,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(itemCode, costComponent, status, fromDate, toDate, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COST_STANDARD_VIEW')")
    @Operation(summary = "获取标准成本详情")
    public ApiResponse<StandardCostVO> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping("/by-item/{itemCode}")
    @PreAuthorize("hasAuthority('COST_STANDARD_VIEW')")
    @Operation(summary = "按物料查询标准成本")
    public ApiResponse<List<StandardCostVO>> listByItem(@PathVariable String itemCode) {
        return ApiResponse.success(service.listByItemCode(itemCode));
    }
}