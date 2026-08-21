-- V30: 清理已废弃的发布模块表 + 调度锁表（从已删除的 V29 迁移）

-- 1) 删除发布模块表（已废弃，功能已移除）
DROP TABLE IF EXISTS rx_deploy_approval;
DROP TABLE IF EXISTS rx_deployment_log;
DROP TABLE IF EXISTS rx_deployment_task;
DROP TABLE IF EXISTS rx_object_lock;
DROP TABLE IF EXISTS rx_deployment;

-- 2) 删除废弃的 deployment.retry 配置项
DELETE FROM rx_config WHERE config_key = 'deployment.retry';

-- 3) 删除废弃的 deploy 菜单项（菜单种子中已移除，此处清理旧数据）
DELETE FROM rx_menu WHERE title = 'deploy';

-- 4) 调度锁表：CollectorScheduler 等定时任务多节点抢锁，仅 Leader 执行
CREATE TABLE IF NOT EXISTS rx_scheduler_lock (
    lock_key   VARCHAR(64) PRIMARY KEY COMMENT '锁键，如 rx_monitor_collector',
    holder     VARCHAR(128) COMMENT '持有节点标识（hostname:pid）',
    acquired_at DATETIME COMMENT '获取时间',
    expires_at  DATETIME COMMENT '过期时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分布式调度锁';

INSERT IGNORE INTO rx_scheduler_lock (lock_key, holder, acquired_at, expires_at)
VALUES ('rx_monitor_collector', NULL, NULL, NULL);