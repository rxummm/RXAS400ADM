-- ============================================================
-- V29（恢复桩，P1-12）：发布模块清理 + 调度锁表
-- 说明：V29 曾被删除并入 V30（见 V30 头部注释），导致版本序列 V28→V30 出现缺口；
--       生产环境 validate-on-migrate=true 时，已应用 V29 的库会报
--       "Detected applied migration not resolved locally" 而拒绝启动。
-- 本文件恢复 V29 为幂等语句（与 V30 内容一致：DROP ... IF EXISTS /
--   CREATE TABLE IF NOT EXISTS / INSERT IGNORE / DELETE 均安全重放）：
--   - 从未应用 V29 的库：顺序执行 V29 → V30，结果一致；
--   - 已应用原 V29 的库：校验通过（若原内容一致），V30 幂等重放无副作用。
-- ============================================================

-- 1) 删除发布模块表（已废弃，功能已移除）
DROP TABLE IF EXISTS rx_deploy_approval;
DROP TABLE IF EXISTS rx_deployment_log;
DROP TABLE IF EXISTS rx_deployment_task;
DROP TABLE IF EXISTS rx_object_lock;
DROP TABLE IF EXISTS rx_deployment;

-- 2) 删除废弃的 deployment.retry 配置项
DELETE FROM rx_config WHERE config_key = 'deployment.retry';

-- 3) 删除废弃的 deploy 菜单项
DELETE FROM rx_menu WHERE title = 'deploy';

-- 4) 调度锁表：CollectorScheduler 等定时任务多节点抢锁，仅 Leader 执行
CREATE TABLE IF NOT EXISTS rx_scheduler_lock (
    lock_key    VARCHAR(64) PRIMARY KEY COMMENT '锁键，如 rx_monitor_collector',
    holder      VARCHAR(128) COMMENT '持有节点标识（hostname:pid）',
    acquired_at DATETIME COMMENT '获取时间',
    expires_at  DATETIME COMMENT '过期时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分布式调度锁';

INSERT IGNORE INTO rx_scheduler_lock (lock_key, holder, acquired_at, expires_at)
VALUES ('rx_monitor_collector', NULL, NULL, NULL);
