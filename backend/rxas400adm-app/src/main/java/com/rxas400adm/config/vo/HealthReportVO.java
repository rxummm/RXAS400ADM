package com.rxas400adm.config.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台健康巡检报告（HealthController.report 返回）。
 */
public record HealthReportVO(
        LocalDateTime generatedAt,
        String database,
        String scheduler,
        List<ServerHealthVO> servers,
        long openAlerts,
        DiskSpaceVO diskSpace,
        MemoryVO memory
) {
    /**
     * 单台服务器连接测试结果。
     */
    public record ServerHealthVO(Long id, String name, String host, String environment, String connect, String detail) {
    }

    /**
     * 磁盘空间信息。
     */
    public record DiskSpaceVO(String status, long total, long free, String totalFormatted, String freeFormatted) {
    }

    /**
     * JVM 内存信息。
     */
    public record MemoryVO(String status, long used, long max, String usedFormatted, String maxFormatted) {
    }
}
