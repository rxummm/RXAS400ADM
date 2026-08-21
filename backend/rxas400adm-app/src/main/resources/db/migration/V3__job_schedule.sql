-- 作业调度（追踪文档 2.4.4）：定时执行 CL 命令 / SQL（Quartz 调度，任务定义持久化）
CREATE TABLE rx_job_schedule (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    description   VARCHAR(255),
    server_id     BIGINT        NOT NULL COMMENT '执行目标 AS400 服务器',
    schedule_type VARCHAR(10)   NOT NULL COMMENT 'CL / SQL',
    command       TEXT          NOT NULL COMMENT 'CL 命令或 SQL 语句',
    cron_expr     VARCHAR(50)   NOT NULL COMMENT 'Quartz cron 表达式',
    enabled       TINYINT(1)    NOT NULL DEFAULT 1,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    last_run_time DATETIME,
    last_result   VARCHAR(500),
    created_by    VARCHAR(50),
    created_time  DATETIME      NOT NULL,
    updated_time  DATETIME,
    KEY idx_schedule_server (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业调度任务';

CREATE TABLE rx_job_schedule_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT       NOT NULL,
    run_time    DATETIME     NOT NULL,
    status      VARCHAR(20)  NOT NULL COMMENT 'SUCCESS / FAILED',
    message     VARCHAR(1000),
    cost_ms     BIGINT,
    KEY idx_history_schedule (schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业调度执行历史';
