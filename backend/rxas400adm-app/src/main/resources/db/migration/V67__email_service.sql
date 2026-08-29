-- V67: 邮件服务增强 — 新建 4 张表 + 权限码/菜单种子 + 数据迁移
-- ============================================================

-- 1. 邮件配置表（替代 rx_config 中的 alert.email.* 键值对）
CREATE TABLE IF NOT EXISTS rx_email_config (
    config_key   VARCHAR(100)  NOT NULL PRIMARY KEY COMMENT '配置键（如 smtp.host）',
    config_value TEXT          NULL     COMMENT '配置值',
    description  VARCHAR(255)  NULL     COMMENT '描述',
    updated_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件服务配置';

-- 2. 收件人分组
CREATE TABLE IF NOT EXISTS rx_email_recipient_group (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_name   VARCHAR(100)  NOT NULL COMMENT '分组名称',
    description  VARCHAR(255)  NULL     COMMENT '描述',
    created_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件收件人分组';

-- 3. 分组成员
CREATE TABLE IF NOT EXISTS rx_email_recipient (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_id     BIGINT        NOT NULL COMMENT '所属分组',
    email        VARCHAR(100)  NOT NULL COMMENT '邮箱地址',
    user_id      BIGINT        NULL     COMMENT '关联用户ID（可选）',
    enabled      TINYINT       NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_group_id (group_id),
    UNIQUE KEY uk_group_email (group_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件分组成员';

-- 4. 发送日志
CREATE TABLE IF NOT EXISTS rx_email_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    subject         VARCHAR(255)  NOT NULL COMMENT '邮件主题',
    recipients      TEXT          NOT NULL COMMENT '收件人列表',
    channel         VARCHAR(20)   NOT NULL COMMENT '渠道：ALERT/REPORT/MANUAL',
    status          VARCHAR(20)   NOT NULL COMMENT '状态：SUCCESS/FAILED',
    error_message   TEXT          NULL     COMMENT '失败原因',
    attachment_name VARCHAR(255)  NULL     COMMENT '附件文件名',
    created_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_channel (channel),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志';

-- 5. 迁移 rx_config 中的 alert.email.* 到 rx_email_config
INSERT IGNORE INTO rx_email_config (config_key, config_value, description)
SELECT REPLACE(config_key, 'alert.email.', ''), config_value, description
FROM rx_config WHERE config_key LIKE 'alert.email.%';

-- 6. 权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description) VALUES
('EMAIL_MANAGE', 'EMAIL_MANAGE', 'SYSTEM', '邮件配置与收件人分组管理'),
('EMAIL_VIEW', 'EMAIL_VIEW', 'SYSTEM', '邮件日志查看'),
('EMAIL_SEND', 'EMAIL_SEND', 'SYSTEM', '独立发送邮件');

-- 7. 菜单：系统管理下新增邮件设置/收件人分组/邮件日志
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1),
       '邮件设置', 2, 'emailConfig', '/system/email-config', 'views/system/emailConfig/index.vue', 'EMAIL_MANAGE', 'Message', 8, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'emailConfig' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1),
       '收件人分组', 2, 'emailGroups', '/system/email-groups', 'views/system/emailGroups/index.vue', 'EMAIL_MANAGE', 'UserFilled', 9, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'emailGroups' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1),
       '邮件日志', 2, 'emailLog', '/system/email-log', 'views/system/emailLog/index.vue', 'EMAIL_VIEW', 'Document', 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'emailLog' AND menu_type = 2);

-- 8. 菜单：邮件中心目录 + 写邮件
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '邮件中心', 1, 'groupMail', '/group-mail', 'Message', 12, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupMail' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMail' AND menu_type = 1 LIMIT 1),
       '写邮件', 2, 'compose', '/mail/compose', 'views/mail/compose.vue', 'EMAIL_SEND', 'Edit', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'compose' AND menu_type = 2);

-- 9. ADMIN 角色授权（菜单 + 权限）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN'
  AND m.title IN ('groupMail', 'compose', 'emailConfig', 'emailGroups', 'emailLog')
  AND m.status = 1;

INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('EMAIL_MANAGE', 'EMAIL_VIEW', 'EMAIL_SEND');
