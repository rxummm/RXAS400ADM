-- ============================================================
-- V12: 历史库角色-菜单授权补发（幂等）
-- V10/V11 新增 区域管理/日历/角色管理 菜单，但老库 rx_role_menu 在更早已初始化
-- （DataInitializer 只在空表时写入，之后不再补），导致这些新菜单未授予任何角色，
-- 非 ADMIN 角色左侧菜单缺失（ADMIN 走全量旁路不受影响）。
-- 授权规则与 DataInitializer.initRoleMenus 保持一致：
--   ADMIN / OPERATOR / DEVELOPER / VIEWER → 区域管理 + 日历
--   ADMIN（仅）→ 角色管理
-- INSERT IGNORE：已存在 (role_id, menu_id) 自动跳过，可重复执行。
-- ============================================================

INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r
JOIN rx_menu m ON m.title IN ('region', 'calendar') AND m.status = 1
WHERE r.role_code IN ('ADMIN', 'OPERATOR', 'DEVELOPER', 'VIEWER');

INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r
JOIN rx_menu m ON m.title = 'roles' AND m.status = 1
WHERE r.role_code = 'ADMIN';
