-- V86: 补充遗漏的 i18n 翻译键

-- users.serverId: 用户安全审计表格「Server」列表头
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('users.serverId', 'zh-CN', '服务器', 'users');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('users.serverId', 'en-US', 'Server', 'users');
