-- ============================================================
-- V35: 补齐高频查询列缺失索引（P1-13）
-- 核对自 8-14 代码审查：以下 WHERE/ORDER BY 列均无索引（information_schema 验证），
-- 列表页/告警查询/登录安全等场景随数据量增长会全表扫描。
-- ============================================================

-- 告警事件：按实例 / 规则 / 状态+时间 检索
CREATE INDEX idx_alert_event_instance ON rx_alert_event (instance_id);
CREATE INDEX idx_alert_event_rule ON rx_alert_event (rule_id);
CREATE INDEX idx_alert_event_status_time ON rx_alert_event (status, created_time);

-- 文档版本：按文档取版本历史
CREATE INDEX idx_doc_version_doc ON rx_doc_version (doc_id);

-- SQL 历史：按操作人检索
CREATE INDEX idx_sql_history_operator ON rx_sql_history (operator);

-- 用户：AS400 登录来源按服务器过滤/统计（登录安全页）
CREATE INDEX idx_user_as400_server ON rx_user (as400_server_id);

-- IBM i 实例：启用列表 + 默认服务器兜底查询
CREATE INDEX idx_ibmi_system_enabled_default ON rx_ibmi_system (enabled, default_server);

-- 告警规则：按服务器过滤
CREATE INDEX idx_alert_rule_server ON rx_alert_rule (server_id);
