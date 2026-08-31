-- AS400用户Profile管理功能
-- 创建用户Profile操作日志表

CREATE TABLE IF NOT EXISTS rx_as400_user_profile_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(10) NOT NULL,
    action VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    detail TEXT COMMENT '操作详情',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_name (user_name),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AS400用户Profile操作日志';

-- 注册权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
    ('USER_PROFILE_VIEW', '查看AS400用户Profile', 'SYSTEM'),
    ('USER_PROFILE_CREATE', '创建AS400用户Profile', 'SYSTEM'),
    ('USER_PROFILE_UPDATE', '修改AS400用户Profile', 'SYSTEM'),
    ('USER_PROFILE_DELETE', '删除AS400用户Profile', 'SYSTEM');

-- 为ADMIN角色分配权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('USER_PROFILE_VIEW', 'USER_PROFILE_CREATE', 'USER_PROFILE_UPDATE', 'USER_PROFILE_DELETE');

-- 添加菜单
-- 父目录：AS400用户管理
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, created_time, updated_time)
SELECT '用户Profile管理', 1, 'userProfileGroup', '/as400/user-profiles', '', 'fa-solid fa-users', 10,
    (SELECT id FROM (SELECT id FROM rx_menu WHERE menu_name = 'AS400管理' AND menu_type = 1 LIMIT 1) AS tmp),
    1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = '用户Profile管理' AND menu_type = 1
);

-- 叶子菜单：用户Profile列表
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, perms, created_time, updated_time)
SELECT '用户Profile列表', 2, 'userProfiles', '/as400/user-profiles', 'views/as400/userProfiles/index.vue', '', 1,
    (SELECT id FROM (SELECT id FROM rx_menu WHERE menu_name = '用户Profile管理' AND menu_type = 1 LIMIT 1) AS tmp),
    1, 1, 0, 'USER_PROFILE_VIEW', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = '用户Profile列表' AND menu_type = 2
);

-- 为ADMIN角色分配菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN'
  AND m.menu_name IN ('用户Profile管理', '用户Profile列表');
