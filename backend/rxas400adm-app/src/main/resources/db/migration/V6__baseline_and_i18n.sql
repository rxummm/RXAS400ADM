-- 性能基线（2.1.9）：每服务器每指标每日均值/峰值/最小值
CREATE TABLE rx_metric_baseline (
    instance_id   BIGINT       NOT NULL,
    metric_name   VARCHAR(20)  NOT NULL,
    baseline_date DATE         NOT NULL,
    avg_value     DOUBLE       NOT NULL DEFAULT 0,
    max_value     DOUBLE       NOT NULL DEFAULT 0,
    min_value     DOUBLE       NOT NULL DEFAULT 0,
    sample_count  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (instance_id, metric_name, baseline_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控指标性能基线';

-- 动态翻译（2.5.2 轻量版）：管理端可维护的 i18n key
CREATE TABLE rx_i18n (
    i18n_key  VARCHAR(100) NOT NULL,
    lang      VARCHAR(10)  NOT NULL COMMENT 'zh-CN / en-US',
    text      VARCHAR(500) NOT NULL,
    PRIMARY KEY (i18n_key, lang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态翻译表';

INSERT IGNORE INTO rx_i18n (i18n_key, lang, text) VALUES
('banner.welcome', 'zh-CN', '欢迎使用 RXAS400 运维平台'),
('banner.welcome', 'en-US', 'Welcome to RXAS400 Operation Platform'),
('banner.health', 'zh-CN', '平台运行健康'),
('banner.health', 'en-US', 'Platform is healthy');