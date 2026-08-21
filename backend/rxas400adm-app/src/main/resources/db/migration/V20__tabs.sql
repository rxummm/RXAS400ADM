-- ============================================================
-- V20: Tab 独立为 menu_type=4（页面内 tab 级授权）
--     模型：目录=1 / 页面=2 / 按钮=3 / Tab=4
--     默认可见策略：perms 为空 → 所有能看该页面的用户都可见；
--     管理员在菜单管理把 perms 填上权限码 → 仅被授权（角色/用户直授）可见，
--     停用（status=0）→ 全局隐藏。
--     1) 「审批管理」从按钮(3)迁移为 Tab(4)（id 不变，既有 rx_role_menu/
--        rx_user_menu 授权与 perms=SYS_PERMISSION_REQUEST 全部保留）
--     2) Job 中心 / 用户管理 / Webhook 管理 三个多 Tab 页面建 Tab 行
-- ============================================================

-- 1) 审批管理：按钮 → Tab（幂等：仅当仍为 menu_type=3 时迁移）
UPDATE rx_menu
SET menu_type = 4, updated_time = NOW()
WHERE id = 47 AND menu_type = 3 AND title = 'permissionRequestReview';

-- 2) Job 中心（jobs）Tab 行
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '作业', 4, 'jobsTab', NULL, NULL, NULL, 'fa-solid fa-list', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'jobs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'jobsTab' AND b.menu_type = 4);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '队列', 4, 'queuesTab', NULL, NULL, NULL, 'fa-solid fa-layer-group', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'jobs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'queuesTab' AND b.menu_type = 4);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '输出', 4, 'spoolTab', NULL, NULL, NULL, 'fa-solid fa-print', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'jobs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'spoolTab' AND b.menu_type = 4);

-- 3) 用户管理（users）Tab 行
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '用户', 4, 'usersTab', NULL, NULL, NULL, 'fa-solid fa-user', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'users' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'usersTab' AND b.menu_type = 4);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '登录安全', 4, 'securityTab', NULL, NULL, NULL, 'fa-solid fa-shield-halved', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'users' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'securityTab' AND b.menu_type = 4);

-- 4) Webhook 管理（webhooks）Tab 行
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '配置', 4, 'configTab', NULL, NULL, NULL, 'fa-solid fa-gear', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'webhooks' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'configTab' AND b.menu_type = 4);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '日志', 4, 'logTab', NULL, NULL, NULL, 'fa-solid fa-file-lines', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'webhooks' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'logTab' AND b.menu_type = 4);
