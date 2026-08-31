package com.rxas400adm.as400.freight;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bpcs/freight-cost")
@RequiredArgsConstructor
@Tag(name = "运费核算与成本分析")
public class FreightCostController {

    private final FreightCostService freightCostService;

    // ==================== 规则管理 ====================

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "查询运费规则列表")
    public ApiResponse<List<FreightCostRuleVO>> listRules() {
        return ApiResponse.success(freightCostService.listRules().stream().map(FreightCostRuleVO::from).toList());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费规则", operation = "新增运费规则")
    @Operation(summary = "新增运费规则")
    public ApiResponse<FreightCostRuleVO> createRule(@Valid @RequestBody FreightCostRuleDTO dto) {
        return ApiResponse.success(FreightCostRuleVO.from(freightCostService.createRule(dto)));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费规则", operation = "更新运费规则")
    @Operation(summary = "更新运费规则")
    public ApiResponse<FreightCostRuleVO> updateRule(@PathVariable Long id, @Valid @RequestBody FreightCostRuleDTO dto) {
        return ApiResponse.success(FreightCostRuleVO.from(freightCostService.updateRule(id, dto)));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费规则", operation = "删除运费规则")
    @Operation(summary = "删除运费规则")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        freightCostService.deleteRule(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/rules/{id}/toggle")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费规则", operation = "启停运费规则")
    @Operation(summary = "启停运费规则")
    public ApiResponse<FreightCostRuleVO> toggleRule(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ApiResponse.success(FreightCostRuleVO.from(freightCostService.toggleRule(id, enabled)));
    }

    // ==================== 运费计算 ====================

    @GetMapping("/calculate")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "计算运费")
    public ApiResponse<BigDecimal> calculate(
            @RequestParam String carrier,
            @RequestParam String costType,
            @RequestParam BigDecimal quantity) {
        return ApiResponse.success(freightCostService.calculateFreight(carrier, costType, quantity));
    }

    // ==================== 运费记录 ====================

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "分页查询运费记录")
    public ApiResponse<Map<String, Object>> listRecords(FreightCostQueryDTO query) {
        var page = freightCostService.listRecords(query);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(FreightCostRecordVO::from).toList());
        result.put("total", page.getTotal());
        return ApiResponse.success(result);
    }

    @PostMapping("/records")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费记录", operation = "新增运费记录")
    @Operation(summary = "新增运费记录")
    public ApiResponse<FreightCostRecordVO> createRecord(@Valid @RequestBody FreightCostRecordDTO dto) {
        return ApiResponse.success(FreightCostRecordVO.from(freightCostService.createRecord(dto)));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "运费记录", operation = "删除运费记录")
    @Operation(summary = "删除运费记录")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        freightCostService.deleteRecord(id);
        return ApiResponse.success(null);
    }

    // ==================== 成本分析 ====================

    @GetMapping("/analysis/trend")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "月度运费趋势")
    public ApiResponse<List<FreightCostTrendVO>> monthlyTrend(
            @RequestParam(required = false) String carrier,
            @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.success(freightCostService.getMonthlyTrend(carrier, months));
    }

    @GetMapping("/analysis/carrier-share")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "承运商运费占比")
    public ApiResponse<Map<String, BigDecimal>> carrierCostShare() {
        return ApiResponse.success(freightCostService.getCarrierCostShare());
    }
}
