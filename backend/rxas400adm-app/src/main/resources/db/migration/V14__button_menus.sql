-- ============================================================
-- V14: 按钮级菜单种子（menu_type=3）
--   按钮权限码随角色-菜单授权（rx_role_menu）下发，参与前端 hasPermission 判断
--   幂等：按 parent 标题 + title + menu_type 去重
-- ============================================================

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT m.id, '作业控制', 3, 'jobControl', NULL, NULL, 'JOB_END', NULL, 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu m
WHERE m.title = 'jobs' AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'jobControl' AND b.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT m.id, '消息应答', 3, 'jobReply', NULL, NULL, 'JOB_END', NULL, 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu m
WHERE m.title = 'jobs' AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'jobReply' AND b.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT m.id, '启停子系统', 3, 'subsystemControl', NULL, NULL, 'SUBSYSTEM_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu m
WHERE m.title = 'subsystems' AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'subsystemControl' AND b.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT m.id, '脚本管理', 3, 'scriptManage', NULL, NULL, 'SCRIPT_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu m
WHERE m.title = 'scripts' AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'scriptManage' AND b.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT m.id, '调度管理', 3, 'scheduleManage', NULL, NULL, 'SCHEDULE_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu m
WHERE m.title = 'schedules' AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'scheduleManage' AND b.menu_type = 3);

-- 角色授权：给 非 VIEWER 角色补绑按钮菜单（按角色编码 → 按钮 title 集合）
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, bm.id
FROM rx_role r
JOIN rx_menu bm ON bm.menu_type = 3
WHERE r.role_code IN ('ADMIN', 'OPERATOR', 'DEVELOPER')
  AND (
    (r.role_code = 'OPERATOR' AND bm.title IN ('jobControl', 'jobReply', 'subsystemControl', 'scriptManage', 'scheduleManage'))
    OR (r.role_code = 'DEVELOPER' AND bm.title IN ('scriptManage', 'scheduleManage'))
    OR r.role_code = 'ADMIN'
  )
  AND NOT EXISTS (
    SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = bm.id
  );
