-- V100：操作模板菜单（挂在「工具」分组下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupTools' AND menu_type = 1 LIMIT 1),
       'opTemplate', 2, 'opTemplate', '/op-templates', 'views/opTemplate/index.vue', 'SCRIPT_MANAGE', 'fa-solid fa-scroll', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'opTemplate' AND menu_type = 2);
