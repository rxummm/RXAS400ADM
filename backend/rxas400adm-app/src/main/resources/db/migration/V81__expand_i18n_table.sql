-- 扩展 rx_i18n 表：增加 module/updated_at/updated_by 字段，支持翻译数据库化
-- 1. 新增字段（带默认值，不锁表）
ALTER TABLE rx_i18n
    ADD COLUMN module     VARCHAR(30)  NOT NULL DEFAULT 'common' COMMENT '模块：menu/bpcs/common/system/validation等' AFTER text,
    ADD COLUMN updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间' AFTER module,
    ADD COLUMN updated_by VARCHAR(50)  NULL     COMMENT '最后修改人' AFTER updated_at;

-- 2. 按 key 前缀批量更新 module 值
UPDATE rx_i18n SET module = 'menu'       WHERE i18n_key LIKE 'menu.%';
UPDATE rx_i18n SET module = 'bpcs'       WHERE i18n_key LIKE 'bpcs.%';
UPDATE rx_i18n SET module = 'system'     WHERE i18n_key LIKE 'system.%' OR i18n_key LIKE 'users.%' OR i18n_key LIKE 'role.%' OR i18n_key LIKE 'permissions.%';
UPDATE rx_i18n SET module = 'monitor'    WHERE i18n_key LIKE 'monitor.%' OR i18n_key LIKE 'alertRules.%' OR i18n_key LIKE 'health.%';
UPDATE rx_i18n SET module = 'jobs'       WHERE i18n_key LIKE 'jobs.%' OR i18n_key LIKE 'schedule.%' OR i18n_key LIKE 'scripts.%';
UPDATE rx_i18n SET module = 'assets'     WHERE i18n_key LIKE 'assets.%' OR i18n_key LIKE 'topology.%';
UPDATE rx_i18n SET module = 'docs'       WHERE i18n_key LIKE 'docs.%' OR i18n_key LIKE 'reports.%' OR i18n_key LIKE 'tool.%';
UPDATE rx_i18n SET module = 'validation' WHERE i18n_key LIKE 'validation.%';
UPDATE rx_i18n SET module = 'common'     WHERE module = 'common' AND (i18n_key LIKE 'common.%' OR i18n_key LIKE 'layout.%' OR i18n_key LIKE 'login.%' OR i18n_key LIKE 'theme.%' OR i18n_key LIKE 'banner.%' OR i18n_key LIKE 'query.%' OR i18n_key LIKE 'objects.%' OR i18n_key LIKE 'ifs.%');

-- 3. 添加模块索引
CREATE INDEX idx_i18n_module ON rx_i18n (module);
