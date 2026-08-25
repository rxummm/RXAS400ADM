package com.rxas400adm.config;

import java.io.File;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.config.vo.HealthReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 平台健康巡检（3.6，参照旧项目 As400HealthCheckController）：\n * 聚合 DB / Quartz 调度器 / 各服务器连接 / 未关闭告警 / 指标采集情况 为一站式巡检报告。\n */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "健康巡检")
public class HealthController {

    private final HealthService healthService;
    private final AS400ClientProvider clientProvider;
    private final Scheduler scheduler;

    @GetMapping
    @PreAuthorize("hasAuthority('HEALTH_VIEW')")
    public ApiResponse<HealthReportVO> report() {
        // 数据库连通性（轻量探测）
        String database = healthService.probeDatabase() ? "OK" : "DOWN";

        // Quartz 调度器状态
        boolean schedulerOk;
        try {
            schedulerOk = scheduler.isStarted();
        } catch (SchedulerException e) {
            schedulerOk = false;
        }
        String schedulerStatus = schedulerOk ? "RUNNING" : "STOPPED";

        // 各服务器连接测试
        List<HealthReportVO.ServerHealthVO> servers = new ArrayList<>();
        for (IbmiSystem system : healthService.listSystemsOrdered()) {
            String connect;
            String detail;
            try {
                AS400Client client = clientProvider.forServer(system.getId());
                com.rxas400adm.as400.CommandResult result = client.testConnection();
                connect = result.success() ? "OK" : "FAIL";
                detail = result.message();
            } catch (Exception e) {
                connect = "FAIL";
                detail = e.getMessage();
            }
            servers.add(new HealthReportVO.ServerHealthVO(
                    system.getId(), system.getName(), system.getHost(),
                    system.getEnvironment(), connect, detail));
        }

        return ApiResponse.success(new HealthReportVO(
                LocalDateTime.now(), database, schedulerStatus,
                servers, healthService.countOpenAlerts(),
                getDiskSpaceInfo(), getMemoryInfo()));
    }

    private HealthReportVO.DiskSpaceVO getDiskSpaceInfo() {
        try {
            File root = new File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            return new HealthReportVO.DiskSpaceVO("UP", total, free,
                    formatBytes(total), formatBytes(free));
        } catch (Exception e) {
            return new HealthReportVO.DiskSpaceVO("DOWN", 0, 0, "0 B", "0 B");
        }
    }

    private HealthReportVO.MemoryVO getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return new HealthReportVO.MemoryVO("UP", used, max,
                formatBytes(used), formatBytes(max));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
