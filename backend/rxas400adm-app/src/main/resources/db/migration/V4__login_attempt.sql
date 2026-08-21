-- 登录失败记录与锁定持久化（2.2.7 增强）：数据库存储（重启不清零），按用户名+服务器维度统计
CREATE TABLE rx_login_attempt (
    username      VARCHAR(50) NOT NULL,
    server_id     BIGINT      NOT NULL DEFAULT 0 COMMENT '0=平台登录；>0=AS400 服务器 ID',
    failed_count  INT         NOT NULL DEFAULT 0,
    locked_until  DATETIME    COMMENT '锁定到期时间，NULL=未锁定',
    last_fail_time DATETIME,
    last_ip       VARCHAR(64),
    updated_time  DATETIME,
    PRIMARY KEY (username, server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录失败记录与账号锁定';
