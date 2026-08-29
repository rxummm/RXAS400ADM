-- V62：数据区域管理菜单与权限种子

-- 权限码
INSERT INTO rx_permission (permission_code, permission_name, module)
SELECT 'DATA_AREA_VIEW', '数据区域查看', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'DATA_AREA_VIEW');

INSERT INTO rx_permission (permission_code, permission_name, module)
SELECT 'DATA_AREA_EDIT', '数据区域管理', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'DATA_AREA_EDIT');

-- 菜单：数据区域（在「系统值」之后）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'sysvals' AND menu_type = 2 LIMIT 1),
    'dataAreas', 2, 'dataAreas', '/data-areas', 'views/data/dataAreas/index.vue', 'DATA_AREA_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'dataAreas' AND menu_type = 2);

-- ADMIN 角色授权
INSERT INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('DATA_AREA_VIEW', 'DATA_AREA_EDIT')
AND NOT EXISTS (SELECT 1 FROM rx_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('dataAreas')
AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
