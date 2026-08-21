-- ============================================================
-- RXAS400 V1 初始 Schema（MySQL 8）
-- 依据《RXAS400 分析总结》数据库设计章节，落地为 MySQL 方言
-- 数据库：rxas400adm（UTF-8）
-- ============================================================

-- ---------- security ----------

CREATE TABLE rx_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    email        VARCHAR(100),
    status       VARCHAR(20)  DEFAULT 'ACTIVE',
    created_by   VARCHAR(50),
    created_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(50) UNIQUE,
    role_name   VARCHAR(100),
    description TEXT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) UNIQUE,
    permission_name VARCHAR(100),
    module          VARCHAR(50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES rx_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES rx_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES rx_role (id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES rx_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_audit_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name    VARCHAR(50),
    action       VARCHAR(50),
    module       VARCHAR(50),
    target       VARCHAR(200),
    ip           VARCHAR(50),
    detail       TEXT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_audit_user_time ON rx_audit_log (user_name, created_time DESC);

-- ---------- as400（多系统管理） ----------

CREATE TABLE rx_ibmi_system (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    host             VARCHAR(100) NOT NULL,
    port             INTEGER      DEFAULT 8470,
    username         VARCHAR(50),
    password_encrypt VARCHAR(500),
    environment      VARCHAR(20),                          -- PROD / TEST / DEV / DR
    region           VARCHAR(50),
    critical_level   VARCHAR(20),
    ha_group         VARCHAR(50),
    ssl_enabled      TINYINT(1)   DEFAULT 0,
    default_libraries VARCHAR(255),
    ccsid            INTEGER      DEFAULT 37,
    enabled          TINYINT(1)   DEFAULT 1,
    default_server   TINYINT(1)   DEFAULT 0,
    status           VARCHAR(20)  DEFAULT 'OFFLINE',        -- ONLINE / OFFLINE / UNKNOWN
    description      TEXT,
    sort_order       INTEGER      DEFAULT 0,
    created_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_as400_connection_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT,
    success     TINYINT(1),
    message     TEXT,
    test_time   DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- source / compile ----------

CREATE TABLE rx_source_library (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    library_name VARCHAR(100),
    description  TEXT,
    system_name  VARCHAR(50),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_source_file (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    library   VARCHAR(100),
    file_name VARCHAR(100),
    type      VARCHAR(50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_source_member (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    library     VARCHAR(100),
    source_file VARCHAR(100),
    member_name VARCHAR(100),
    source_type VARCHAR(50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_compile_record (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    library      VARCHAR(100),
    source_file  VARCHAR(100),
    member       VARCHAR(100),
    command      VARCHAR(100),
    status       VARCHAR(20),
    message      TEXT,
    operator     VARCHAR(50),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- monitor ----------

CREATE TABLE rx_metric (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id  BIGINT,
    metric_type  VARCHAR(50),
    metric_name  VARCHAR(100),
    metric_value DOUBLE,
    collect_time DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
-- 说明：MySQL 下高频指标表建议按月分区（PARTITION BY RANGE (TO_DAYS(collect_time))），
-- 后续按 rx_metric 数据量实施

CREATE INDEX idx_metric_instance_time ON rx_metric (instance_id, collect_time DESC);

CREATE TABLE rx_job_history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT,
    job_name    VARCHAR(100),
    job_user    VARCHAR(50),
    job_number  VARCHAR(20),
    status      VARCHAR(20),
    cpu_time    BIGINT,
    record_time DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_job_instance ON rx_job_history (instance_id, record_time);

CREATE TABLE rx_alert_rule (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_name      VARCHAR(100),
    operator         VARCHAR(10),
    threshold        DOUBLE,
    duration_seconds INTEGER,
    level            VARCHAR(20),
    enabled          TINYINT(1) DEFAULT 1
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_alert_event (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id  BIGINT,
    rule_id      BIGINT,
    level        VARCHAR(20),
    message      TEXT,
    status       VARCHAR(20),
    created_time DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- system ----------

CREATE TABLE rx_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description  TEXT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO rx_config (config_key, config_value, description)
VALUES ('monitor.interval', '10', '监控采集间隔（秒）');