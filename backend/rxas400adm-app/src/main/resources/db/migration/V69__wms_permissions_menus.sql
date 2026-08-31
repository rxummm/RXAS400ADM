-- V69: WMS 仓库管理 — 权限 + 菜单 + BPCS SQL 说明

-- 1. 权限
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('BPCS_WMS_VIEW',        '仓库/库位总览', 'BPCS'),
('BPCS_WMS_BATCH_VIEW',  '批次追踪', 'BPCS');

-- 2. 菜单（BPCS 供应链 → 仓库管理子菜单）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, m_data.menu_name, 2, m_data.title, m_data.path, m_data.component, m_data.perms, m_data.icon, m_data.sort_val, 1, 1, 0, NOW(), NOW()
FROM (SELECT id FROM rx_menu WHERE path='/bpcs' LIMIT 1) p
CROSS JOIN (
  SELECT '仓库总览' AS menu_name, 'wmsOverview' AS title, '/bpcs/wms/overview' AS path, 'views/bpcs/wmsOverview/index.vue' AS component, 'BPCS_WMS_VIEW' AS perms, 'House' AS icon, 1 AS sort_val
  UNION ALL SELECT '库位库存', 'wmsBinInventory', '/bpcs/wms/binInventory', 'views/bpcs/wmsBinInventory/index.vue', 'BPCS_WMS_VIEW', 'Box', 2
  UNION ALL SELECT '拣货路径', 'wmsPickPath', '/bpcs/wms/pickPath', 'views/bpcs/wmsPickPath/index.vue', 'BPCS_WMS_VIEW', 'Guide', 3
  UNION ALL SELECT '批次追踪', 'wmsBatchTracking', '/bpcs/wms/batchTracking', 'views/bpcs/wmsBatchTracking/index.vue', 'BPCS_WMS_BATCH_VIEW', 'Tickets', 4
  UNION ALL SELECT '库存移动记录', 'wmsMovementHistory', '/bpcs/wms/movementHistory', 'views/bpcs/wmsMovementHistory/index.vue', 'BPCS_WMS_VIEW', 'Sort', 5
) m_data
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = m_data.title AND menu_type = 2);

-- 3. ADMIN 角色授权
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('BPCS_WMS_VIEW', 'BPCS_WMS_BATCH_VIEW');

-- 4. ADMIN 角色菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('wmsOverview', 'wmsBinInventory', 'wmsPickPath', 'wmsBatchTracking', 'wmsMovementHistory');
