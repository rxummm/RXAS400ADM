-- ============================================================
-- V16: 数据字典（类型 + 字典项 两级 CRUD）
-- ============================================================
CREATE TABLE IF NOT EXISTS rx_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    code VARCHAR(50) NOT NULL COMMENT '字典类型编码（如 job_status）',
    name VARCHAR(100) NOT NULL COMMENT '字典类型名称',
    remark VARCHAR(255) NULL COMMENT '备注',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_by VARCHAR(50) NULL,
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    UNIQUE KEY uk_dict_code (code)
) COMMENT '数据字典类型';

CREATE TABLE IF NOT EXISTS rx_dict_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    type_code VARCHAR(50) NOT NULL COMMENT '字典类型编码',
    item_key VARCHAR(50) NOT NULL COMMENT '字典项键（value）',
    item_value VARCHAR(200) NOT NULL COMMENT '字典项名称（label）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    UNIQUE KEY uk_dict_item (type_code, item_key)
) COMMENT '数据字典项';

-- 系统管理组「字典管理」菜单（旧库；新库由 DataInitializer 写入）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '字典管理', 2, 'dict', '/system/dict', 'views/system/dict/index.vue', 'DICT_MANAGE', 'fa-solid fa-book', 12, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu)
  AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'dict' AND menu_type = 2);

-- 示例字典：作业状态 / 服务器环境
INSERT INTO rx_dict_type (code, name, remark, sort, status, created_time, updated_time)
SELECT 'job_status', '作业状态', 'Job 中心活动作业状态', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_type WHERE code = 'job_status');

INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'job_status', 'RUN', '运行中', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'job_status' AND item_key = 'RUN');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'job_status', 'MSGW', '等待消息', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'job_status' AND item_key = 'MSGW');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'job_status', 'HELD', '已挂起', 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'job_status' AND item_key = 'HELD');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'job_status', 'LCKW', '死锁等待', 4, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'job_status' AND item_key = 'LCKW');

INSERT INTO rx_dict_type (code, name, remark, sort, status, created_time, updated_time)
SELECT 'server_env', '服务器环境', 'AS400 服务器环境标签', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_type WHERE code = 'server_env');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'server_env', 'PROD', '生产', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'server_env' AND item_key = 'PROD');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'server_env', 'TEST', '测试', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'server_env' AND item_key = 'TEST');
INSERT INTO rx_dict_item (type_code, item_key, item_value, sort, status, created_time, updated_time)
SELECT 'server_env', 'DEV', '开发', 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_dict_item WHERE type_code = 'server_env' AND item_key = 'DEV');
