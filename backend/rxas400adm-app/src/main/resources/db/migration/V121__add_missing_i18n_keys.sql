-- 补充 7 处硬编码 label 对应的 i18n key（code review N-3 修复）
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('schedule.cron',       'zh-CN', 'Cron 表达式',  'schedule'),
('operation.id',        'zh-CN', 'ID',           'operation'),
('notice.type',         'zh-CN', '类型',         'notice'),
('permissionRequest.user', 'zh-CN', '申请人',    'permissionRequest'),
('disruption.riskLevel','zh-CN', '风险等级',     'bpcs');

INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('schedule.cron',       'en-US', 'Cron Expression',  'schedule'),
('operation.id',        'en-US', 'ID',               'operation'),
('notice.type',         'en-US', 'Type',             'notice'),
('permissionRequest.user', 'en-US', 'Applicant',     'permissionRequest'),
('disruption.riskLevel','en-US', 'Risk Level',       'bpcs');
