package com.rxas400adm.monitor.scheduler;

import com.rxas400adm.monitor.service.IMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 指标历史保留策略（2.1.7）：每日凌晨清理超过 {@code rxas400.monitor.retention-days} 天的指标。
 * （按月分区属部署级 DDL，生产环境可另行在表上追加 PARTITION。）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricRetentionJob {

    private final IMetricService metricService;

    @Value("${rxas400.monitor.retention-days:30}")
    private int retentionDays;

    /** 每天 03:30 执行 */
    @Scheduled(cron = "0 30 3 * * *")
    public void clean() {
        int days = Math.max(1, retentionDays);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        try {
            metricService.cleanBefore(cutoff);
            log.info("[指标保留] 已清理 {} 天前的指标（截止 {}）", days, cutoff);
        } catch (Exception e) {
            log.warn("[指标保留] 清理失败: {}", e.getMessage());
        }
    }
}