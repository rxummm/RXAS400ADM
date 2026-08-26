package com.rxas400adm.monitor.scheduler;

import com.rxas400adm.monitor.mapper.MetricMapper;
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

    /** P18：单批删除行数上限（MySQL LIMIT 分批，避免一次性 DELETE 全量的大事务长锁） */
    private static final int BATCH_SIZE = 5000;

    private final MetricMapper metricMapper;

    @Value("${rxas400.monitor.retention-days:30}")
    private int retentionDays;

    /** 每天 03:30 执行 */
    @Scheduled(cron = "0 30 3 * * *")
    public void clean() {
        int days = Math.max(1, retentionDays);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        try {
            // P18：分批循环删除直至影响行数为 0（批间无需 sleep）；
            // DB2 for i 方言下 deleteByCreatedTimeBefore 内部退回一次性整删，首轮即清空
            long total = 0L;
            int affected;
            do {
                affected = metricMapper.deleteByCreatedTimeBefore(cutoff, BATCH_SIZE);
                total += affected;
            } while (affected > 0);
            log.info("[指标保留] 已清理 {} 天前的指标 {} 行（截止 {}）", days, total, cutoff);
        } catch (Exception e) {
            // 【E5-4】追加异常对象，保留完整堆栈（原仅拼 getMessage 丢堆栈）
            log.warn("[指标保留] 清理失败: {}", e.getMessage(), e);
        }
    }
}