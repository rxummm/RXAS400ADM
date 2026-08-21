-- ============================================================================
-- P3：DB 低收益项收敛
--   1) rx_audit_log.ip VARCHAR(50) → VARCHAR(255)：
--      X-Forwarded-For 链（可信代理白名单场景）可能超过 50 字符，加宽防截断；
--   2) rx_metric 增加 metric_name 索引：告警引擎按指标名检索最近采样，
--      与既有 (instance_id, collect_time) 索引互补；
--   3) 删除 rx_script.favorite 低基数索引：布尔列选择性极低，优化器不会走，
--      保留只会增加写放大（MySQL 8 对低选择性索引直接忽略）。
-- ============================================================================

ALTER TABLE rx_audit_log MODIFY ip VARCHAR(255);

CREATE INDEX idx_metric_name_time ON rx_metric (metric_name, collect_time DESC);

-- 仅当索引存在时删除（幂等）
SET @drop_idx := (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'rx_script'
                    AND index_name = 'idx_script_favorite');
SET @sql := IF(@drop_idx > 0, 'ALTER TABLE rx_script DROP INDEX idx_script_favorite', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
