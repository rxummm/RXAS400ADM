package com.rxas400adm.cost.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.cost.service.ProfitAnalysisService;
import com.rxas400adm.cost.vo.ProfitAnalysisVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cost/profit-analysis")
@RequiredArgsConstructor
@Tag(name = "利润分析", description = "产品利润分析")
public class ProfitAnalysisController {

    private final ProfitAnalysisService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COST_PROFIT_VIEW')")
    @Operation(summary = "分页查询利润分析")
    public ApiResponse<PageResult<ProfitAnalysisVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String analysisType,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, analysisType, period, current, size)
                .map(ProfitAnalysisVO::from));
    }
}
