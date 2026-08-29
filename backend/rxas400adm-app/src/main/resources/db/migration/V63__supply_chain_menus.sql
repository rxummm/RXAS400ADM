-- V63：供应链增强菜单种子（订单列表 / 库存预警 / 销售分析）

-- 订单列表
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsOrderList', 2, 'bpcsOrderList', '/bpcs-order-list', 'views/bpcs/orderList/index.vue', 'BPCS_ORDER_VIEW', NULL, 9, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderList' AND menu_type = 2);

-- 库存预警
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsInventoryAlert', 2, 'bpcsInventoryAlert', '/bpcs-inventory-alert', 'views/bpcs/inventoryAlert/index.vue', 'BPCS_INVENTORY_VIEW', NULL, 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInventoryAlert' AND menu_type = 2);

-- 销售分析
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsSalesAnalysis', 2, 'bpcsSalesAnalysis', '/bpcs-sales-analysis', 'views/bpcs/salesAnalysis/index.vue', 'BPCS_SALES_VIEW', NULL, 11, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsSalesAnalysis' AND menu_type = 2);

-- ADMIN 角色授权
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcsOrderList', 'bpcsInventoryAlert', 'bpcsSalesAnalysis')
AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
