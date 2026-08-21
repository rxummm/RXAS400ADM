-- ============================================================
-- V24: 权限码可管理化
--     1) rx_permission 增加 description 列（权限说明）
--     2) 既有权限码 module 改为按前缀推导的模块（JOB_VIEW→JOB、
--        SYS_CONFIG_MANAGE→SYS_CONFIG、SYS_PERMISSION_REQUEST→SYS_PERMISSION）
--        供「按菜单匹配」的 perms 下拉选择过滤使用
--     3) 新增「权限码管理」页面菜单（menu_type=2，perms=PERMISSION_MANAGE）
-- ============================================================

ALTER TABLE rx_permission ADD COLUMN description VARCHAR(200) NULL COMMENT '权限说明';

-- 模块 = 权限码最后一个下划线之前的部分（JOB_VIEW→JOB，SYS_CONFIG_MANAGE→SYS_CONFIG）
UPDATE rx_permission
SET module = CASE
    WHEN permission_code LIKE '%\_%'
        THEN LEFT(permission_code,
                  CHAR_LENGTH(permission_code) - CHAR_LENGTH(SUBSTRING_INDEX(permission_code, '_', -1)) - 1)
    ELSE 'SYSTEM'
END
WHERE module IS NULL OR module = '' OR module = 'SYSTEM';

-- 权限码管理页（挂在「系统管理」目录下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '权限码管理', 2, 'permissions', '/system/permissions', 'views/system/permissions/index.vue', 'PERMISSION_MANAGE', 'fa-solid fa-key', 13, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'permissions' AND m.menu_type = 2);
