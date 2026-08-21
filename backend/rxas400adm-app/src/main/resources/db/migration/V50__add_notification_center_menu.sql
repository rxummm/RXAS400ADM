-- V50: 添加通知中心菜单（/system/notifications），修复铃铛「查看全部」无权限问题
-- 通知中心页面路由已在前端注册，但缺少对应的 rx_menu 菜单项，
-- 导致路由守卫根据菜单路径校验时一律拒绝（含 ADMIN 角色）。

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1),
       '通知中心', 2, 'notifications', '/system/notifications',
       'views/system/notifications/index.vue', NULL, 'fa-solid fa-bell', 15, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'notifications' AND menu_type = 2);