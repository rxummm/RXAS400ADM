-- V74: BPCS 业务增强全部页面菜单种子 + 权限码注册
-- 幂等：INSERT IGNORE / WHERE NOT EXISTS

-- 0. 权限码注册（BPCS_VIEW / BPCS_MANAGE）
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module)
VALUES ('BPCS_VIEW', 'BPCS供应链查看', 'BPCS');
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module)
VALUES ('BPCS_MANAGE', 'BPCS供应链管理', 'BPCS');

-- 0.1 ADMIN 角色授予新权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('BPCS_VIEW', 'BPCS_MANAGE');

-- 0.2 OPERATOR 角色授予 BPCS_VIEW（只读）
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code = 'BPCS_VIEW';

-- Batch 2: 核心能力建设
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '异常监控', 2, 'bpcsAnomaly', '/bpcs-anomaly', 'views/bpcs/anomaly/index.vue', 'BPCS_VIEW', 15, 'fa-solid fa-triangle-exclamation', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsAnomaly' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'BOM管理', 2, 'bpcsBom', '/bpcs-bom', 'views/bpcs/bom/index.vue', 'BPCS_VIEW', 16, 'fa-solid fa-sitemap', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsBom' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '发货管理', 2, 'bpcsShipmentMgmt', '/bpcs-shipment-mgmt', 'views/bpcs/shipmentMgmt/index.vue', 'BPCS_VIEW', 17, 'fa-solid fa-truck', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsShipmentMgmt' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '客户管理', 2, 'bpcsRcmx', '/bpcs-rcmx', 'views/bpcs/rcmx/index.vue', 'BPCS_VIEW', 18, 'fa-solid fa-user-tie', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsRcmx' AND menu_type = 2);

-- Batch 3: 深度优化
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '工作台配置', 2, 'bpcsWabp', '/bpcs-wabp', 'views/bpcs/wabp/index.vue', 'BPCS_VIEW', 19, 'fa-solid fa-sliders', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsWabp' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '库位库存', 2, 'bpcsLocationInv', '/bpcs-location-inv', 'views/bpcs/locationInv/index.vue', 'BPCS_VIEW', 20, 'fa-solid fa-map-location-dot', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsLocationInv' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '补货管理', 2, 'bpcsReplenishment', '/bpcs-replenishment', 'views/bpcs/replenishment/index.vue', 'BPCS_VIEW', 21, 'fa-solid fa-boxes-stacked', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsReplenishment' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '预警引擎', 2, 'bpcsAlertEngine', '/bpcs-alert-engine', 'views/bpcs/alertEngine/index.vue', 'BPCS_VIEW', 22, 'fa-solid fa-bell', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsAlertEngine' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '库存价值', 2, 'bpcsStockValue', '/bpcs-stock-value', 'views/bpcs/stockValue/index.vue', 'BPCS_VIEW', 23, 'fa-solid fa-dollar-sign', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsStockValue' AND menu_type = 2);

-- Batch 4: 扩展功能
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '供应商评分', 2, 'bpcsSupplierScore', '/bpcs-supplier-score', 'views/bpcs/supplierScore/index.vue', 'BPCS_VIEW', 24, 'fa-solid fa-star', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsSupplierScore' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '采购订单生命周期', 2, 'bpcsPoLifecycle', '/bpcs-po-lifecycle', 'views/bpcs/poLifecycle/index.vue', 'BPCS_VIEW', 25, 'fa-solid fa-file-invoice', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsPoLifecycle' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '信用管控', 2, 'bpcsCreditHold', '/bpcs-credit-hold', 'views/bpcs/creditHold/index.vue', 'BPCS_VIEW', 26, 'fa-solid fa-hand', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsCreditHold' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '看板管理', 2, 'bpcsKanban', '/bpcs-kanban', 'views/bpcs/kanban/index.vue', 'BPCS_VIEW', 27, 'fa-solid fa-columns', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsKanban' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '运输看板', 2, 'bpcsTransportDashboard', '/bpcs-transport-dashboard', 'views/bpcs/transportDashboard/index.vue', 'BPCS_VIEW', 28, 'fa-solid fa-gauge-high', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsTransportDashboard' AND menu_type = 2);

-- Batch 5: 长期规划
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单模板', 2, 'bpcsOrderTemplate', '/bpcs-order-template', 'views/bpcs/orderTemplate/index.vue', 'BPCS_VIEW', 29, 'fa-solid fa-clipboard', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderTemplate' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单复制', 2, 'bpcsOrderCopy', '/bpcs-order-copy', 'views/bpcs/orderCopy/index.vue', 'BPCS_MANAGE', 30, 'fa-solid fa-copy', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderCopy' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单变更', 2, 'bpcsOrderChange', '/bpcs-order-change', 'views/bpcs/orderChange/index.vue', 'BPCS_VIEW', 31, 'fa-solid fa-pen-to-square', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderChange' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单报表', 2, 'bpcsOrderReport', '/bpcs-order-report', 'views/bpcs/orderReport/index.vue', 'BPCS_VIEW', 32, 'fa-solid fa-chart-bar', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderReport' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '退货管理', 2, 'bpcsRma', '/bpcs-rma', 'views/bpcs/rma/index.vue', 'BPCS_MANAGE', 33, 'fa-solid fa-rotate-left', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsRma' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单排程', 2, 'bpcsOrderSchedule', '/bpcs-order-schedule', 'views/bpcs/orderSchedule/index.vue', 'BPCS_VIEW', 34, 'fa-solid fa-calendar-days', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderSchedule' AND menu_type = 2);

-- 补充：forecast / abcXyz / cycleCount 菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '需求预测', 2, 'bpcsForecast', '/bpcs-forecast', 'views/bpcs/forecast/index.vue', 'BPCS_VIEW', 35, 'fa-solid fa-chart-line', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsForecast' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    'ABC/XYZ分析', 2, 'bpcsAbcXyz', '/bpcs-abc-xyz', 'views/bpcs/abcXyz/index.vue', 'BPCS_VIEW', 36, 'fa-solid fa-table-cells', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsAbcXyz' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '循环盘点', 2, 'bpcsCycleCount', '/bpcs-cycle-count', 'views/bpcs/cycleCount/index.vue', 'BPCS_VIEW', 37, 'fa-solid fa-clipboard-check', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsCycleCount' AND menu_type = 2);

-- ADMIN：挂载全部新增 BPCS 菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN (
    'bpcsAnomaly', 'bpcsBom', 'bpcsShipmentMgmt', 'bpcsRcmx',
    'bpcsWabp', 'bpcsLocationInv', 'bpcsReplenishment', 'bpcsAlertEngine',
    'bpcsStockValue', 'bpcsSupplierScore', 'bpcsPoLifecycle', 'bpcsCreditHold',
    'bpcsKanban', 'bpcsTransportDashboard', 'bpcsOrderTemplate', 'bpcsOrderCopy',
    'bpcsOrderChange', 'bpcsOrderReport', 'bpcsRma', 'bpcsOrderSchedule',
    'bpcsForecast', 'bpcsAbcXyz', 'bpcsCycleCount'
);

-- OPERATOR：挂载全部 BPCS 浏览页面（BPCS_VIEW 菜单）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'OPERATOR' AND m.title IN (
    'bpcsAnomaly', 'bpcsBom', 'bpcsShipmentMgmt', 'bpcsRcmx',
    'bpcsWabp', 'bpcsLocationInv', 'bpcsReplenishment', 'bpcsAlertEngine',
    'bpcsStockValue', 'bpcsSupplierScore', 'bpcsPoLifecycle', 'bpcsCreditHold',
    'bpcsKanban', 'bpcsTransportDashboard', 'bpcsOrderTemplate',
    'bpcsOrderChange', 'bpcsOrderReport', 'bpcsOrderSchedule',
    'bpcsForecast', 'bpcsAbcXyz', 'bpcsCycleCount'
);
