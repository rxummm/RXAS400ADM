package com.rxas400adm.config;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.config.vo.HealthReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 平台健康巡检（3.6，参照旧项目 As400HealthCheckController）：\n * 聚合 DB / Quartz 调度器 / 各服务器连接 / 未关闭告警 / 指标采集情况 为一站式巡检报告。
 * <p>
 * 【R2】编排逻辑已整体下沉 HealthService.reportWithServers()（含并行探测+30s 缓存），
 * 本类只保留参数接收 + 委托 + ApiResponse 包装。
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "健康巡检")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @PreAuthorize("hasAuthority('HEALTH_VIEW')")
    public ApiResponse<HealthReportVO> report() {
        return ApiResponse.success(healthService.reportWithServers());
    }
}
