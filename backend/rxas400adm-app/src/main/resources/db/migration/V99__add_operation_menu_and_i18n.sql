-- ============================================================
-- V99: Add Operation module menu entry and seed i18n keys
-- ============================================================

-- ---------- 1. Add Operation menu entry under groupTools ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupTools' AND menu_type = 1 LIMIT 1),
       '操作中心', 2, 'operation', '/operations', 'views/operation/index.vue', 'OPERATION_VIEW', 'Cog', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'operation' AND menu_type = 2);

-- ---------- 2. Seed menu.operation i18n translation ----------
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('menu.operation', 'zh-CN', '操作中心', 'menu'),
('menu.operation', 'en-US', 'Operations', 'menu');

-- ---------- 3. Seed operation.* module i18n translations ----------
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('operation.type', 'zh-CN', '操作类型', 'operation'),
('operation.status', 'zh-CN', '状态', 'operation'),
('operation.currentStep', 'zh-CN', '当前步骤', 'operation'),
('operation.riskLevel', 'zh-CN', '风险等级', 'operation'),
('operation.requestedBy', 'zh-CN', '发起人', 'operation'),
('operation.requestedAt', 'zh-CN', '发起时间', 'operation'),
('operation.detail', 'zh-CN', '操作详情', 'operation'),
('operation.cancel', 'zh-CN', '取消操作', 'operation'),
('operation.retry', 'zh-CN', '重试', 'operation');

INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('operation.type', 'en-US', 'Operation Type', 'operation'),
('operation.status', 'en-US', 'Status', 'operation'),
('operation.currentStep', 'en-US', 'Current Step', 'operation'),
('operation.riskLevel', 'en-US', 'Risk Level', 'operation'),
('operation.requestedBy', 'en-US', 'Requested By', 'operation'),
('operation.requestedAt', 'en-US', 'Requested At', 'operation'),
('operation.detail', 'en-US', 'Operation Detail', 'operation'),
('operation.cancel', 'en-US', 'Cancel', 'operation'),
('operation.retry', 'en-US', 'Retry', 'operation');