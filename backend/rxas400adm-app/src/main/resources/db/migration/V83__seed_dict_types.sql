-- V83: 种子字典数据（前端可数据库化枚举值）
-- 10 个字典类型 + 对应字典项，替代前端硬编码 el-option

-- ============================================================
-- 1. ENVIRONMENT（环境类型）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('ENVIRONMENT', '环境类型', 'IBM i 服务器环境分类', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ENVIRONMENT', 'PROD', '生产环境', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ENVIRONMENT', 'TEST', '测试环境', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ENVIRONMENT', 'DEV', '开发环境', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ENVIRONMENT', 'DR', '灾备环境', 4, 1);

-- ============================================================
-- 2. CRITICAL_LEVEL（关键级别）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('CRITICAL_LEVEL', '关键级别', '服务器关键级别分类', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('CRITICAL_LEVEL', 'CRITICAL', '关键', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('CRITICAL_LEVEL', 'NORMAL', '普通', 2, 1);

-- ============================================================
-- 3. METRIC_TYPE（监控指标类型）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('METRIC_TYPE', '监控指标类型', 'IBM i 监控指标分类', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'CPU', 'CPU 使用率', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'MEMORY', '内存使用率', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'DISK', '磁盘使用率', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'NETWORK', '网络流量', 4, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'MSGW', '消息等待', 5, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'LCKW', '锁等待', 6, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('METRIC_TYPE', 'PRINTER', '打印机队列', 7, 1);

-- ============================================================
-- 4. ALERT_LEVEL（告警级别）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('ALERT_LEVEL', '告警级别', '监控告警级别分类', 4, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_LEVEL', 'WARNING', '警告', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_LEVEL', 'CRITICAL', '严重', 2, 1);

-- ============================================================
-- 5. ALERT_CHANNEL（告警通知渠道）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('ALERT_CHANNEL', '告警通知渠道', '监控告警通知方式', 5, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_CHANNEL', 'NONE', '仅站内', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_CHANNEL', 'EMAIL', '邮件', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_CHANNEL', 'WEBHOOK', 'Webhook', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('ALERT_CHANNEL', 'ALL', 'Webhook + 邮件', 4, 1);

-- ============================================================
-- 6. EXEC_STATUS（执行结果状态）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('EXEC_STATUS', '执行结果状态', '脚本/命令执行结果', 6, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('EXEC_STATUS', 'SUCCESS', '成功', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('EXEC_STATUS', 'FAILED', '失败', 2, 1);

-- ============================================================
-- 7. DOC_TYPE（文档类型）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('DOC_TYPE', '文档类型', '文档管理支持的文件类型', 7, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_TYPE', 'MARKDOWN', 'Markdown', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_TYPE', 'TEXT', '纯文本', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_TYPE', 'HTML', 'HTML', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_TYPE', 'PDF', 'PDF', 4, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_TYPE', 'IMAGE', '图片', 5, 1);

-- ============================================================
-- 8. DOC_STATUS（文档状态）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('DOC_STATUS', '文档状态', '文档审批流程状态', 8, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_STATUS', 'DRAFT', '草稿', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_STATUS', 'PENDING', '待审批', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_STATUS', 'PUBLISHED', '已发布', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('DOC_STATUS', 'REJECTED', '已驳回', 4, 1);

-- ============================================================
-- 9. SPECIAL_AUTH（IBM i 特殊权限）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('SPECIAL_AUTH', 'IBM i 特殊权限', 'AS400 用户特殊权限码', 9, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('SPECIAL_AUTH', '*ALLOBJ', '全部对象权限', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('SPECIAL_AUTH', '*SAVRST', '保存恢复权限', 2, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('SPECIAL_AUTH', '*SERVICE', '服务权限', 3, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('SPECIAL_AUTH', '*SECADM', '安全管理员', 4, 1);

-- ============================================================
-- 10. USER_STATUS（IBM i 用户状态）
-- ============================================================
INSERT IGNORE INTO rx_dict_type (code, name, remark, sort, status) VALUES ('USER_STATUS', 'IBM i 用户状态', 'AS400 用户启用/禁用状态', 10, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('USER_STATUS', '*ENABLED', '启用', 1, 1);
INSERT IGNORE INTO rx_dict_item (type_code, item_key, item_value, sort, status) VALUES ('USER_STATUS', '*DISABLED', '禁用', 2, 1);
