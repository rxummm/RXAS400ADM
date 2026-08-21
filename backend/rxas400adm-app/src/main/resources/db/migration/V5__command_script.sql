-- 命令脚本中心（2.4.3）：保存/复用 CL 命令，含收藏与标签
CREATE TABLE rx_command_script (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    description   VARCHAR(255),
    command       TEXT          NOT NULL COMMENT 'CL 命令',
    favorite      TINYINT(1)    NOT NULL DEFAULT 0,
    tags          VARCHAR(255)  COMMENT '逗号分隔标签',
    created_by    VARCHAR(50),
    run_count     INT           NOT NULL DEFAULT 0,
    last_run_time DATETIME,
    last_result   VARCHAR(500),
    created_time  DATETIME      NOT NULL,
    updated_time  DATETIME,
    KEY idx_script_favorite (favorite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命令脚本中心';
