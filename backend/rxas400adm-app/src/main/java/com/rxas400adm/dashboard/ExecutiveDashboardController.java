package com.rxas400adm.dashboard;

import com.rxas400adm.as400.vo.ExecutiveSummaryVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executive Dashboard（高管仪表盘）。
 * 聚合系统监控 + BPCS 业务数据，提供一站式 KPI 总览。
 * 权限：EXECUTIVE_DASHBOARD（只读）。
 */
@RestController
@RequestMapping("/api/v1/dashboard/executive")
@RequiredArgsConstructor
@Tag(name = "Executive Dashboard", description = "高管仪表盘：系统监控 + 业务 KPI 汇总")
public class ExecutiveDashboardController {

    private final ExecutiveDashboardService executiveDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('EXECUTIVE_DASHBOARD')")
    @Operation(summary = "Executive Dashboard 汇总数据")
    public ApiResponse<ExecutiveSummaryVO> summary(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(executiveDashboardService.getSummary(cono));
    }
}
