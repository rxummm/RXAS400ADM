-- V60：【AS400 业务增强·P2】发票轨迹 / 销售趋势 / 采购订单 —— 权限码与菜单种子
-- 幂等：全部 INSERT IGNORE / WHERE NOT EXISTS

-- 1. 权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('BPCS_INVOICE_VIEW', 'BPCS_INVOICE_VIEW', 'SYSTEM'),
('BPCS_SALES_VIEW', 'BPCS_SALES_VIEW', 'SYSTEM'),
('BPCS_PURCHASE_VIEW', 'BPCS_PURCHASE_VIEW', 'SYSTEM');

-- 2. 叶子菜单（挂已有的 AS400 业务目录）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '发票轨迹', 2, 'bpcsInvoice', '/bpcs-invoice', 'BPCS_INVOICE_VIEW', NULL, 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInvoice' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '销售趋势', 2, 'bpcsSales', '/bpcs-sales', 'BPCS_SALES_VIEW', NULL, 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsSales' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '采购订单', 2, 'bpcsPurchase', '/bpcs-purchase', 'BPCS_PURCHASE_VIEW', NULL, 7, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsPurchase' AND menu_type = 2);

-- 3. ADMIN 权限授予
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('BPCS_INVOICE_VIEW', 'BPCS_SALES_VIEW', 'BPCS_PURCHASE_VIEW');

-- 4. ADMIN 菜单挂载
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcsInvoice', 'bpcsSales', 'bpcsPurchase');
