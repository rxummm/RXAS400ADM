-- ============================================================
-- V18: 「审批管理」tab 建模为按钮级菜单（menu_type=3）
--     挂在「权限申请」菜单下，perms=SYS_PERMISSION_REQUEST。
--     这样角色管理页可勾选授权（rx_role_menu），
--     前端 tab 显隐 + 后端 @PreAuthorize 均由授权驱动，不再写死给 ADMIN。
-- ============================================================
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '审批管理', 3, 'permissionRequestReview', NULL, NULL, 'SYS_PERMISSION_REQUEST', 'fa-solid fa-check', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'permissionRequest' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu b WHERE b.title = 'permissionRequestReview' AND b.menu_type = 3);
