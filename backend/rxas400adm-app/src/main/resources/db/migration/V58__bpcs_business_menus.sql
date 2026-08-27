-- V58：【AS400 业务增强·P1】BPCS 客户订单时间轴 —— 权限码与菜单种子
-- 幂等：全部 INSERT … WHERE NOT EXISTS / INSERT IGNORE（对齐 V38 风格）

-- 1. 权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module)
VALUES ('BPCS_ORDER_VIEW', 'BPCS_ORDER_VIEW', 'SYSTEM');

-- 2. 菜单：目录「AS400 业务」
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, 'AS400业务', 1, 'bpcs', '/bpcs', NULL, 'fa-solid fa-chart-line', 60, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1);

-- 3. 叶子菜单「订单时间轴」（挂 AS400 业务目录）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '订单时间轴', 2, 'bpcsOrder', '/bpcs-order', 'views/bpcs/order/index.vue', 'BPCS_ORDER_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrder' AND menu_type = 2);

-- 4. 授权：ADMIN 全量权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'BPCS_ORDER_VIEW';

-- 5. 授权：ADMIN 挂载新目录与新叶子（status=1）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcs', 'bpcsOrder');
