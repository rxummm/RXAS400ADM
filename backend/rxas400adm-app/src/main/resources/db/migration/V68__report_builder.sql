-- V68: 自定义报表构建器 — 报表定义表 + 权限/菜单

-- 1. 报表定义表
CREATE TABLE IF NOT EXISTS rx_report_definition (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL COMMENT '报表名称（唯一）',
    data_source  VARCHAR(50)  NOT NULL COMMENT '数据源标识',
    title        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '报表标题',
    columns_json TEXT         NOT NULL COMMENT '选中列 JSON 数组',
    filters_json TEXT         NULL COMMENT '筛选条件 JSON 数组',
    sorts_json   TEXT         NULL COMMENT '排序规则 JSON 数组',
    created_by   VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report_def_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义报表定义';

-- 2. 权限
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('REPORT_BUILDER_VIEW',  '查看自定义报表', 'SYSTEM'),
('REPORT_BUILDER_MANAGE','管理自定义报表定义', 'SYSTEM');

-- 3. 菜单（报表中心 → 自定义报表 子菜单）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '自定义报表', 2, 'reportBuilder', '/report/builder', 'views/reportBuilder/index.vue', 'REPORT_BUILDER_VIEW', 'DataAnalysis', 1, 1, 1, 0, NOW(), NOW()
FROM (SELECT id FROM rx_menu WHERE path='/report' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title='reportBuilder' AND menu_type=2);

-- 4. ADMIN 角色授权
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('REPORT_BUILDER_VIEW', 'REPORT_BUILDER_MANAGE');

-- 5. ADMIN 角色菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title = 'reportBuilder';
