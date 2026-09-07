package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsForecastService;
import com.rxas400adm.as400.vo.BpcsCpfrVO;
import com.rxas400adm.as400.vo.BpcsForecastVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ⑥ 预测补货看板 Controller。
 * 权限：BPCS_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/forecast")
@RequiredArgsConstructor
@Tag(name = "BPCS Forecast & Replenish")
public class BpcsForecastController {

    private final IBpcsForecastService forecastService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "获取预测数据")
    public ApiResponse<BpcsForecastVO> getForecast(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(required = false) String item,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(forecastService.getForecast(cono, item, months));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "获取物料选项列表")
    public ApiResponse<List<Map<String, String>>> getItemOptions(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(forecastService.getItemOptions(cono, limit));
    }

    @GetMapping("/cpfr")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "协同需求预测分析（CPFR）")
    public ApiResponse<BpcsCpfrVO> getCpfrAnalysis(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(required = false) String item,
            @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.success(forecastService.getCpfrAnalysis(cono, item, months));
    }
}
