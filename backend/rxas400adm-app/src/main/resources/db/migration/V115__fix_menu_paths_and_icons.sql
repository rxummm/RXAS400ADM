-- V115: 修复菜单路径和图标问题
-- 问题1: 采购管理 404 — 旧条目 /procurement/po 和 /procurement/approval 与前端路由不匹配
-- 问题2: 财务管理默认图标 — title 仍为 'ar' 或 icon 缺失
-- 问题3: 监控路径修正 — /server-compare → /monitor/server-compare, /inspection → /monitor/inspection
-- 问题4: 监控告警路径修正 — /monitor/alerts → /monitor/alert-rules
-- 用 SET @var 避免 MySQL 1093

-- ============================================================
-- 预读 parent_id 到变量
-- ============================================================
SET @bpcsProcurement_id = (SELECT id FROM rx_menu WHERE title = 'bpcsProcurement' AND menu_type = 1 LIMIT 1);

-- ============================================================
-- 1. 采购管理：删除旧条目，确保正确条目存在
-- ============================================================

-- 删除旧的 procurement 父条目（路径不匹配前端路由）
DELETE FROM rx_role_menu WHERE menu_id IN (
    SELECT id FROM (SELECT id FROM rx_menu WHERE title = 'procurement' AND menu_type = 1) t
);
DELETE FROM rx_menu WHERE title = 'procurement' AND menu_type = 1;

-- 删除旧的子条目（/procurement/po, /procurement/approval）
DELETE FROM rx_role_menu WHERE menu_id IN (
    SELECT id FROM (SELECT id FROM rx_menu WHERE title IN ('procurementPo', 'procurementApproval')
      AND path IN ('/procurement/po', '/procurement/approval')) t
);
DELETE FROM rx_menu WHERE title IN ('procurementPo', 'procurementApproval')
  AND path IN ('/procurement/po', '/procurement/approval');

-- 确保正确条目存在（挂在 bpcsProcurement 下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcsProcurement_id, '采购订单管理', 2, 'procurementPo', '/procurement-po', 'views/procurement/po/index.vue', 'PO_MANAGE', NULL, 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementPo' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcsProcurement_id, '采购审批中心', 2, 'procurementApproval', '/procurement-approval', 'views/procurement/approval/index.vue', 'PO_APPROVE', NULL, 11, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementApproval' AND menu_type = 2);

-- ============================================================
-- 2. 财务管理：修正 title 和图标
-- ============================================================

-- 更新财务管理父目录（V101 创建为 title='ar'）
UPDATE rx_menu SET title = 'finance', menu_name = '财务管理', path = '/finance', icon = 'fa-solid fa-landmark', updated_time = NOW()
WHERE title = 'ar' AND menu_type = 1;

-- 确保财务管理子菜单 icon 正确
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice', updated_time = NOW()
WHERE title = 'arInvoice' AND menu_type = 2;

-- ============================================================
-- 3. 监控模块路径修正
-- ============================================================

-- 修正 server-compare 路径
UPDATE rx_menu SET path = '/monitor/server-compare', updated_time = NOW()
WHERE title = 'serverCompare' AND menu_type = 2 AND path != '/monitor/server-compare';

-- 修正 inspection 路径
UPDATE rx_menu SET path = '/monitor/inspection', updated_time = NOW()
WHERE title = 'inspection' AND menu_type = 2 AND path != '/monitor/inspection';

-- 修正 alert-rules 路径
UPDATE rx_menu SET path = '/monitor/alert-rules', updated_time = NOW()
WHERE title = 'monitorAlerts' AND menu_type = 2 AND path != '/monitor/alert-rules';

-- ============================================================
-- 4. 确保 ADMIN 角色有权限访问修正后的菜单
-- ============================================================

INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN'
  AND m.title IN ('procurementPo', 'procurementApproval')
  AND m.menu_type = 2;
