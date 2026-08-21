-- ============================================================
-- V23: 对象详情/拓扑弹窗内的引用分析 tab 建模为 menu_type=4
--     此前这些弹窗 tab 未建模 → canSeeTab 恒返回 true（恒可见）。
--     建模后与 Job/Users/Webhook 的 tab 同一套策略：
--     perms 空=默认可见，管理员可在菜单管理填权限码/停用控制。
--     对象搜索页（objects）: refIn(引用入) / refOut(引用出) / authorities(对象权限)
--     拓扑分析页（topology）: refIn / refOut
-- ============================================================

-- 对象搜索（objects）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '引用入', 4, 'refIn', NULL, NULL, NULL, 'fa-solid fa-right-to-bracket', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'objects' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'refIn' AND b.menu_type = 4 AND b.parent_id = p.id);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '引用出', 4, 'refOut', NULL, NULL, NULL, 'fa-solid fa-right-from-bracket', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'objects' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'refOut' AND b.menu_type = 4 AND b.parent_id = p.id);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '对象权限', 4, 'authorities', NULL, NULL, NULL, 'fa-solid fa-key', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'objects' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'authorities' AND b.menu_type = 4 AND b.parent_id = p.id);

-- 拓扑分析（topology）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '引用入', 4, 'refIn', NULL, NULL, NULL, 'fa-solid fa-right-to-bracket', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'topology' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'refIn' AND b.menu_type = 4 AND b.parent_id = p.id);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '引用出', 4, 'refOut', NULL, NULL, NULL, 'fa-solid fa-right-from-bracket', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'topology' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'refOut' AND b.menu_type = 4 AND b.parent_id = p.id);
