-- ============================================================
-- RXAS400 V2：双登录方式（平台 / AS400 user profile）+ SQL 查询历史
-- ============================================================

-- sys_user 增加登录来源与关联 AS400 服务器
ALTER TABLE rx_user
    ADD COLUMN login_source VARCHAR(20) DEFAULT 'PLATFORM' COMMENT '登录来源: PLATFORM / AS400',
    ADD COLUMN as400_server_id BIGINT NULL COMMENT 'AS400 登录来源服务器 ID';

CREATE INDEX idx_user_login_source ON rx_user (login_source);

-- QSYS2 SQL 执行历史（数据查询工具）
CREATE TABLE rx_sql_history (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    sql_text      TEXT,
    rows_returned INTEGER,
    cost_ms       BIGINT,
    operator      VARCHAR(50),
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
