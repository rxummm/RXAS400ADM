-- H2：采集分布式锁从 MySQL GET_LOCK/RELEASE_LOCK 迁移为表级原子锁。
-- GET_LOCK 是 MySQL 专属函数，DB2 for i 无对应（迁移阻塞点）。
-- 表锁实现（UPDATE ... WHERE holder IS NULL OR expires_at < CURRENT_TIMESTAMP，行影响数=1 即抢到）在两库均可原子执行。
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT IGNORE，重复执行安全。
CREATE TABLE IF NOT EXISTS rx_dist_lock (
    lock_key     VARCHAR(64)  NOT NULL COMMENT '锁名称',
    holder       VARCHAR(64)  NULL COMMENT '持有者标识（节点+随机数）',
    acquired_at  TIMESTAMP    NULL COMMENT '获取时间',
    expires_at   TIMESTAMP    NULL COMMENT '过期时间',
    PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式锁（采集 Leader 选举，兼容 MySQL/DB2 for i）';

INSERT IGNORE INTO rx_dist_lock (lock_key, holder, acquired_at, expires_at)
VALUES ('rx_monitor_collector', NULL, NULL, NULL);