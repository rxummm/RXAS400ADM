-- ============================================================
-- V11: 角色-菜单授权（参照旧项目 sys_role_menu）
-- 角色管理页可给角色授权菜单访问；左侧菜单 = 启用(status=1) ∩ 角色已授权
-- ============================================================

-- 角色-菜单关联表
CREATE TABLE rx_role_menu (
    role_id   BIGINT NOT NULL COMMENT '角色 ID',
    menu_id   BIGINT NOT NULL COMMENT '菜单 ID',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_role_menu_menu (menu_id)
) COMMENT '角色-菜单授权表（rx_role_menu）';

-- rx_role 补充运维字段（参照旧项目 sys_role）
ALTER TABLE rx_role
    ADD COLUMN sort   INT NOT NULL DEFAULT 0 COMMENT '排序',
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用';

-- 「角色管理」菜单（幂等，挂系统管理组下；title 需与前端 i18n menu.roles 对应）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT id, '角色管理', 2, 'roles', '/roles', 'views/system/roles/index.vue', 'ROLE_MANAGE', 'UserFilled', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu WHERE title = 'system' AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'roles');

-- 菜单管理排序顺延（角色管理插在其前，幂等）
UPDATE rx_menu SET sort = 3 WHERE title = 'menus' AND sort <= 2;
