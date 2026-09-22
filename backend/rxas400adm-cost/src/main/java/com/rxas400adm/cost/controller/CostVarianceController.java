package com.rxas400adm.cost.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.cost.service.CostVarianceService;
import com.rxas400adm.cost.vo.CostVarianceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cost/variances")
@RequiredArgsConstructor
@Tag(name = "成本差异", description = "成本差异分析")
public class CostVarianceController {

    private final CostVarianceService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COST_VARIANCE_VIEW')")
    @Operation(summary = "分页查询成本差异")
    public ApiResponse<PageResult<CostVarianceVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String costComponent,
            @RequestParam(required = false) String varianceType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, itemCode, costComponent, varianceType, period, status, current, size)
                .map(CostVarianceVO::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COST_VARIANCE_VIEW')")
    @Operation(summary = "获取成本差异详情")
    public ApiResponse<CostVarianceVO> get(@PathVariable Long id) {
        return ApiResponse.success(CostVarianceVO.from(service.getById(id)));
    }
}
