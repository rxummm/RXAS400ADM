-- ============================================================
-- V8: 菜单管理（表驱动动态菜单，参照旧项目 sys_menu）
-- 通过 status 字段控制菜单显示/隐藏，无需改代码
-- ============================================================

CREATE TABLE rx_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT       NULL COMMENT '父菜单 ID（0 或 NULL=顶级）',
    menu_name   VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    menu_type   TINYINT      NOT NULL DEFAULT 2 COMMENT '1=目录 2=菜单 3=按钮',
    title       VARCHAR(64)  NOT NULL COMMENT 'i18n key（前端 $t(menu.title)）',
    path        VARCHAR(128) NULL COMMENT '路由路径',
    component   VARCHAR(128) NULL COMMENT '前端组件（懒加载路径）',
    perms       VARCHAR(128) NULL COMMENT '权限码（如 MONITOR_VIEW）',
    icon        VARCHAR(64)  NULL COMMENT '图标名（Element Plus）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=可见 0=隐藏',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用(显示) 0=停用(隐藏)',
    admin_only  TINYINT      NOT NULL DEFAULT 0 COMMENT '1=仅管理员可见',
    created_time DATETIME    NULL,
    updated_time DATETIME    NULL,
    INDEX idx_menu_parent (parent_id),
    INDEX idx_menu_status (status)
) COMMENT '菜单表（表驱动动态菜单）';
