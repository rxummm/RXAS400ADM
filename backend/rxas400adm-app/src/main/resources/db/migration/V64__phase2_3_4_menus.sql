-- V64：Phase 2-4 供应链增强菜单种子

-- Phase 2: 库存变动历史
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsInventoryHistory', 2, 'bpcsInventoryHistory', '/bpcs-inventory-history', 'views/bpcs/inventoryHistory/index.vue', 'BPCS_INVENTORY_VIEW', NULL, 12, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInventoryHistory' AND menu_type = 2);

-- Phase 2: 采购收货管理
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsPurchaseReceiving', 2, 'bpcsPurchaseReceiving', '/bpcs-purchase-receiving', 'views/bpcs/purchaseReceiving/index.vue', 'BPCS_PURCHASE_VIEW', NULL, 13, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsPurchaseReceiving' AND menu_type = 2);

-- Phase 2: 发运列表视图
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsShippingList', 2, 'bpcsShippingList', '/bpcs-shipping-list', 'views/bpcs/shippingList/index.vue', 'BPCS_SHIPPING_VIEW', NULL, 14, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsShippingList' AND menu_type = 2);

-- Phase 3: ABC 分析
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsAbcAnalysis', 2, 'bpcsAbcAnalysis', '/bpcs-abc-analysis', 'views/bpcs/abcAnalysis/index.vue', 'BPCS_INVENTORY_VIEW', NULL, 15, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsAbcAnalysis' AND menu_type = 2);

-- Phase 3: 供应商绩效
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsSupplierPerf', 2, 'bpcsSupplierPerf', '/bpcs-supplier-perf', 'views/bpcs/supplierPerf/index.vue', 'BPCS_PURCHASE_VIEW', NULL, 16, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsSupplierPerf' AND menu_type = 2);

-- Phase 4: 供应链 KPI
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsKpi', 2, 'bpcsKpi', '/bpcs-kpi', 'views/bpcs/kpi/index.vue', 'BPCS_ORDER_VIEW', NULL, 17, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsKpi' AND menu_type = 2);

-- Phase 4: 订单全链路追踪
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'bpcsOrderTracking', 2, 'bpcsOrderTracking', '/bpcs-order-tracking', 'views/bpcs/orderTracking/index.vue', 'BPCS_ORDER_VIEW', NULL, 18, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderTracking' AND menu_type = 2);

-- ADMIN 角色授权
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcsInventoryHistory','bpcsPurchaseReceiving','bpcsShippingList','bpcsAbcAnalysis','bpcsSupplierPerf','bpcsKpi','bpcsOrderTracking')
AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
