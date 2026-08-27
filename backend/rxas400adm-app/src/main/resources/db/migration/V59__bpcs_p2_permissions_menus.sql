-- V59：【AS400 业务增强·P2】客户档案 / 库存可用量 / 发运看板 —— 权限码与菜单种子
-- 幂等：全部 INSERT IGNORE / WHERE NOT EXISTS

-- 1. 权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('BPCS_CUSTOMER_VIEW', 'BPCS_CUSTOMER_VIEW', 'SYSTEM'),
('BPCS_INVENTORY_VIEW', 'BPCS_INVENTORY_VIEW', 'SYSTEM'),
('BPCS_SHIPPING_VIEW', 'BPCS_SHIPPING_VIEW', 'SYSTEM');

-- 2. 叶子菜单（挂已有的 AS400 业务目录）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '客户档案', 2, 'bpcsCustomer', '/bpcs-customer', 'BPCS_CUSTOMER_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsCustomer' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '库存可用量', 2, 'bpcsInventory', '/bpcs-inventory', 'BPCS_INVENTORY_VIEW', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInventory' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
       '发运看板', 2, 'bpcsShipping', '/bpcs-shipping', 'BPCS_SHIPPING_VIEW', NULL, 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsShipping' AND menu_type = 2);

-- 3. ADMIN 权限授予
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('BPCS_CUSTOMER_VIEW', 'BPCS_INVENTORY_VIEW', 'BPCS_SHIPPING_VIEW');

-- 4. ADMIN 菜单挂载
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcsCustomer', 'bpcsInventory', 'bpcsShipping');
