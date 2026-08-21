-- P3：指标报表聚合下推（ReportService.metricsRows → MetricMapper.selectAggregatedMetrics）
-- 新查询按 instance_id + collect_time 范围过滤后按日期+指标分组。
-- V41 的 idx_metric_name_time (metric_name, collect_time DESC) 服务于监控采集/告警侧，
-- 此处补充 (instance_id, collect_time) 覆盖报表查询，避免全表扫描。
-- 幂等：先查 information_schema 再建，重复执行安全。
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'rx_metric'
      AND index_name = 'idx_metric_instance_time'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE rx_metric ADD INDEX idx_metric_instance_time (instance_id, collect_time)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
