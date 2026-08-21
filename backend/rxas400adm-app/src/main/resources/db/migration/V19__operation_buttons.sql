-- ============================================================
-- V19: 操作类权限码建模为按钮级菜单（menu_type=3）
--     覆盖：COMPILE_EXECUTE / DOC_MANAGE / DOC_APPROVE /
--           CALENDAR_MANAGE / REGION_MANAGE / NOTIFICATION_MANAGE
--     这些码没有挂靠的页面菜单（页面 perms 为空或为 VIEW 码），
--     需独立按钮行才能在角色管理页勾选授权（rx_role_menu）。
--
--     说明：QUERY_EXECUTE / SYS_CONFIG_MANAGE / WEBHOOK_MANAGE
--           等码已直接挂在页面菜单（menu_type=2）上，随 2.5.10 的
--           userMenuPerms（页面 perms 也并入后端 authorities）随页面授权生效，
--           无需重复按钮行。
--
--     另：清空「权限申请」页面菜单(39)的 perms=SYS_PERMISSION_REQUEST，
--         该码归「审批管理」按钮(47)独占——避免「授权申请页 = 获得审批能力」。
-- ============================================================

-- 1) 清空权限申请页面菜单的 perms（幂等：仅当仍为 SYS_PERMISSION_REQUEST 时）
UPDATE rx_menu
SET perms = NULL, updated_time = NOW()
WHERE title = 'permissionRequest' AND menu_type = 2 AND perms = 'SYS_PERMISSION_REQUEST';

-- 2) 编译执行（挂在「源代码」菜单下；source 页当前 status=0 隐藏，恢复后按钮即生效）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '编译执行', 3, 'compileExecute', NULL, NULL, 'COMPILE_EXECUTE', 'fa-solid fa-code', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'source' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'compileExecute' AND b.menu_type = 3);

-- 3) 文档管理 + 文档审批（挂在「文档管理」页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '文档管理', 3, 'docManage', NULL, NULL, 'DOC_MANAGE', 'fa-solid fa-pen-to-square', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'docs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'docManage' AND b.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '文档审批', 3, 'docApprove', NULL, NULL, 'DOC_APPROVE', 'fa-solid fa-check-double', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'docs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'docApprove' AND b.menu_type = 3);

-- 4) 日历管理（挂在「日历」页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '日历管理', 3, 'calendarManage', NULL, NULL, 'CALENDAR_MANAGE', 'fa-solid fa-calendar-plus', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'calendar' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'calendarManage' AND b.menu_type = 3);

-- 5) 区域管理（挂在「区域管理」页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '区域管理', 3, 'regionManage', NULL, NULL, 'REGION_MANAGE', 'fa-solid fa-location-dot', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'region' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'regionManage' AND b.menu_type = 3);

-- 6) 通知管理（站内信删除/批量删除；挂在「系统管理」目录下，无独立页面菜单）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '通知管理', 3, 'notificationManage', NULL, NULL, 'NOTIFICATION_MANAGE', 'fa-solid fa-bell', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'notificationManage' AND b.menu_type = 3);
