package com.rxas400adm.monitor.controller;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.monitor.vo.AlertEventVO;
import com.rxas400adm.monitor.service.AlertEventService;
import com.rxas400adm.monitor.service.IBaselineService;
import com.rxas400adm.monitor.service.ICapacityService;
import com.rxas400adm.monitor.service.IMetricService;
import com.rxas400adm.monitor.service.MonitorService;
import com.rxas400adm.monitor.vo.BaselineDataVO;
import com.rxas400adm.monitor.vo.CapacityTrendVO;
import com.rxas400adm.monitor.vo.CompareResultVO;
import com.rxas400adm.monitor.vo.MetricVO;
import com.rxas400adm.monitor.vo.OverviewDataVO;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "监控中心", description = "指标查询 / 告警事件 / 容量分析 / 基线管理")
@Tag(name = "监控中心")
public class MonitorController {

    private final IMetricService metricService;
    private final AlertEventService alertEventService;
    private final ICapacityService capacityService;
    private final IBaselineService baselineService;
    private final MonitorService monitorService;

    @GetMapping("/overview/{id}")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<OverviewDataVO> overview(@PathVariable Long id) {
        return ApiResponse.success(OverviewDataVO.from(metricService.overview(id)));
    }

    @GetMapping("/metrics/{id}")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<List<MetricVO>> metrics(@PathVariable Long id,
                                               @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(metricService.history(id, limit).stream().map(MetricVO::from).toList());
    }

    /** 容量规划：DISK 趋势 + 线性回归预测（2.3.6） */
    @GetMapping("/capacity")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<CapacityTrendVO> capacity(@RequestParam Long instanceId,
                                                   @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(CapacityTrendVO.from(capacityService.trend(instanceId, days)));
    }

    /** 性能基线：最近基线 + 当前值偏差（2.1.9） */
    @GetMapping("/baseline/{id}")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<List<BaselineDataVO>> baseline(@PathVariable Long id) {
        baselineService.computeBaseline(id);
        return ApiResponse.success(baselineService.baselineWithCurrent(id).stream()
                .map(BaselineDataVO::from).toList());
    }

    /** 服务器对比：多服务器当前指标快照并排（借鉴旧项目 serverCompare，指标维度）
     *  【R2】【P4】组装与并行编排下沉 MonitorService.compare（P2-17 截断逻辑随迁） */
    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<List<CompareResultVO>> compare(@RequestParam List<Long> ids) {
        return ApiResponse.success(monitorService.compare(ids));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<List<AlertEventVO>> alerts(@RequestParam(defaultValue = "50") int limit) {
        // 按创建时间倒序取最近 N 条（避免全表查出再内存截断）
        return ApiResponse.success(alertEventService.recent(limit).stream().map(AlertEventVO::from).toList());
    }
}
