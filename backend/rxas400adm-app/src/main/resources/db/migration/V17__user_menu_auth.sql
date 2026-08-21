-- ============================================================
-- V17: 用户-菜单直接授权（参照旧项目 sys_user_menu）
--     管理员可对单个用户直接授权菜单/按钮，叠加在角色授权之上；
--     权限申请审批通过后也写入本表。
-- ============================================================
CREATE TABLE IF NOT EXISTS rx_user_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    menu_id BIGINT NOT NULL COMMENT '菜单/按钮 ID（rx_menu.id）',
    created_time DATETIME NULL,
    UNIQUE KEY uk_user_menu (user_id, menu_id),
    KEY idx_um_menu (menu_id)
) COMMENT '用户-菜单直接授权';

-- 权限申请表补菜单维度（兼容原权限码申请）；permission_code 允许为空（菜单树模式下不填）
ALTER TABLE rx_permission_request
    ADD COLUMN menu_ids VARCHAR(2000) NULL COMMENT '申请的菜单/按钮 ID 列表（JSON 数组）' AFTER permission_code,
    ADD COLUMN menu_names VARCHAR(2000) NULL COMMENT '申请的菜单名称列表（JSON 数组，用于审批展示）' AFTER menu_ids,
    MODIFY COLUMN permission_code VARCHAR(100) NULL COMMENT '申请权限码（旧方式，可为空）';
