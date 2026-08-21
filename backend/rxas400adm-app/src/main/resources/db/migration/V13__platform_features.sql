-- ============================================================
-- V13: 平台增强功能（12 项子集落地）
--   Webhook 管理 + 发送日志 / 站内通知 / 系统公告 / 权限申请
--   IP 黑白名单 / 仪表盘 Widget 偏好 / 系统管理扩展菜单
-- 菜单行仅在菜单表已有数据时执行（全新库由 DataInitializer 种子写入，避免双写）
-- ============================================================

-- ---- Webhook 配置 ----
CREATE TABLE rx_webhook (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    url VARCHAR(500) NOT NULL COMMENT '推送地址',
    secret VARCHAR(200) NULL COMMENT '签名密钥',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    description VARCHAR(255) NULL COMMENT '描述',
    created_by VARCHAR(50) NULL COMMENT '创建人',
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    UNIQUE KEY uk_webhook_name (name)
) COMMENT 'Webhook 推送配置';

-- ---- Webhook 发送日志 ----
CREATE TABLE rx_webhook_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    webhook_id BIGINT NULL COMMENT 'webhook 配置 id',
    webhook_name VARCHAR(100) NULL COMMENT '触发时名称（删除配置后仍可追溯）',
    title VARCHAR(255) NULL COMMENT '标题',
    message TEXT NULL COMMENT '内容',
    success TINYINT NOT NULL DEFAULT 0 COMMENT '1=成功 0=失败',
    attempts INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
    error_msg VARCHAR(500) NULL COMMENT '失败原因',
    created_time DATETIME NULL,
    INDEX idx_weblog_time (created_time),
    INDEX idx_weblog_webhook (webhook_id)
) COMMENT 'Webhook 发送日志';

-- ---- 站内通知（按用户） ----
CREATE TABLE rx_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '接收人',
    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT 'ALERT/NOTICE/SYSTEM/PERMISSION',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    content TEXT NULL COMMENT '内容',
    read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '1=已读 0=未读',
    created_time DATETIME NULL,
    INDEX idx_notif_user (username, read_flag),
    INDEX idx_notif_time (created_time)
) COMMENT '站内通知';

-- ---- 系统公告 ----
CREATE TABLE rx_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=发布 0=下架',
    published_time DATETIME NULL COMMENT '发布时间',
    created_by VARCHAR(50) NULL COMMENT '创建人',
    created_time DATETIME NULL,
    updated_time DATETIME NULL
) COMMENT '系统公告';

-- ---- 权限申请 ----
CREATE TABLE rx_permission_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '申请人',
    permission_code VARCHAR(100) NOT NULL COMMENT '申请权限码',
    reason VARCHAR(500) NULL COMMENT '申请原因',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    approver VARCHAR(50) NULL COMMENT '审批人',
    approve_comment VARCHAR(500) NULL COMMENT '审批意见',
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    INDEX idx_preq_user (username),
    INDEX idx_preq_status (status)
) COMMENT '权限自助申请';

-- ---- IP 黑白名单 ----
CREATE TABLE rx_ip_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    ip VARCHAR(64) NOT NULL COMMENT 'IP（支持通配 * 与 CIDR，如 192.168.1.* / 10.0.0.0/8）',
    type VARCHAR(10) NOT NULL COMMENT 'BLACK=黑名单 WHITE=白名单',
    description VARCHAR(255) NULL COMMENT '描述',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_by VARCHAR(50) NULL COMMENT '创建人',
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    UNIQUE KEY uk_iprule (ip, type)
) COMMENT '登录 IP 黑白名单';

-- ---- 仪表盘 Widget 偏好（按用户） ----
CREATE TABLE rx_dashboard_widget (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '用户',
    widget_key VARCHAR(50) NOT NULL COMMENT 'widget 标识（cpu/mem/traffic/alert/task）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=显示 0=隐藏',
    UNIQUE KEY uk_dw_user (username, widget_key)
) COMMENT '仪表盘 Widget 显示偏好';

-- ---- 系统管理扩展菜单（仅旧库；全新库由 DataInitializer 写入） ----
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '系统配置', 2, 'config', '/system/config', 'views/system/config/index.vue', 'SYS_CONFIG_MANAGE', 'fa-solid fa-sliders', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'config');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '翻译管理', 2, 'i18n', '/system/i18n', 'views/system/i18n/index.vue', 'I18N_MANAGE', 'fa-solid fa-globe', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'i18n');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '登录日志', 2, 'loginLog', '/system/login-log', 'views/system/loginLog/index.vue', 'AUDIT_VIEW', 'fa-solid fa-clock-rotate-left', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'loginLog');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '缓存管理', 2, 'cache', '/system/cache', 'views/system/cache/index.vue', 'SYS_CACHE_MANAGE', 'fa-solid fa-bolt', 4, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'cache');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '定时任务', 2, 'tasks', '/system/tasks', 'views/system/tasks/index.vue', 'SYS_TASK_MANAGE', 'fa-solid fa-clock', 5, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tasks');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, 'IP 黑白名单', 2, 'ipRules', '/system/ip-rules', 'views/system/ipRules/index.vue', 'SYS_IP_MANAGE', 'fa-solid fa-shield', 6, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ipRules');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, 'Webhook 管理', 2, 'webhooks', '/system/webhooks', 'views/system/webhooks/index.vue', 'WEBHOOK_MANAGE', 'fa-solid fa-envelope', 7, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'webhooks');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '权限申请', 2, 'permissionRequest', '/system/permission-request', 'views/system/permissionRequest/index.vue', 'SYS_PERMISSION_REQUEST', 'fa-solid fa-key', 8, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'permissionRequest');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '通知公告', 2, 'notices', '/system/notices', 'views/system/notice/index.vue', 'NOTICE_MANAGE', 'fa-solid fa-bullhorn', 9, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND EXISTS (SELECT 1 FROM rx_menu) AND NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'notices');

-- ---- 迁移旧配置 alert.webhook.url → rx_webhook（避免升级后告警推送静默失效） ----
INSERT INTO rx_webhook (name, url, enabled, description, created_by, created_time, updated_time)
SELECT '默认告警 Webhook', config_value, 1, '由 alert.webhook.url 迁移而来', 'system', NOW(), NOW()
FROM rx_config
WHERE config_key = 'alert.webhook.url'
  AND config_value IS NOT NULL AND config_value <> ''
  AND NOT EXISTS (SELECT 1 FROM rx_webhook);