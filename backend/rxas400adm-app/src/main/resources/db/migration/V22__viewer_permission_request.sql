-- ============================================================
-- V22: VIEWER 补授「权限申请」页（menu 39）
--     权限申请是自助流程的核心入口（用户在此申请页面/tab/按钮权限），
--     VIEWER 作为只读角色必须能看到该页；DataInitializer 新库种子已含，
--     但既有库的 VIEWER 授权缺失，此处幂等补齐。
-- ============================================================
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r CROSS JOIN rx_menu m
WHERE r.role_code = 'VIEWER'
  AND m.title = 'permissionRequest'
  AND m.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm
                  WHERE rm.role_id = r.id AND rm.menu_id = m.id);
