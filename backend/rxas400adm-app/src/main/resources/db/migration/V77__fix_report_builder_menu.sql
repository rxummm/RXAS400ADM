-- V77: 修复 reportBuilder 菜单（V68 用 path='/report' 查父节点，实际 reports 菜单 path='/reports'，导致菜单未创建、权限缺失）

-- 1. 创建 reportBuilder 菜单（正确关联到 reports 页面）
INSERT IGNORE INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '自定义报表', 2, 'reportBuilder', '/report/builder', 'views/report/ReportBuilder.vue', 'REPORT_BUILDER_VIEW', 'DataAnalysis', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'reports' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'reportBuilder' AND menu_type = 2);

-- 2. ADMIN 角色授权权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('REPORT_BUILDER_VIEW', 'REPORT_BUILDER_MANAGE');

-- 3. ADMIN 角色授权菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title = 'reportBuilder';

-- 4. 修正 V68 可能写入的错误 component 路径
UPDATE rx_menu SET component = 'views/report/ReportBuilder.vue' WHERE title = 'reportBuilder' AND component = 'views/reportBuilder/index.vue';
