-- ============================================================
-- V39: P2-20 FK 策略统一
-- V1 建了 rx_user_role / rx_role_permission 的 FK，其余表全无外键，
-- rx_job_schedule.server_id / rx_user.as400_server_id 等可长期孤儿。
-- 本次：① 先清理存量孤儿数据 ② 为关键外键统一加约束 ③ 顺带修复
-- rx_metric_baseline.metric_name VARCHAR(20) 比 rx_metric 的 VARCHAR(100) 窄的问题。
--
-- 删除策略（在迁移注释中固化，避免后续误解）：
-- - 服务器被删 → 其作业调度/调度历史/服务器级告警规则/报表任务级联删除
--   （指向已删除服务器的配置是死配置，不应保留为全局规则而误伤告警语义）
-- - 服务器被删 → rx_user.as400_server_id 置 NULL（保留用户，仅解除登录来源绑定）
-- - 调度被删 → 其执行历史级联删除（应用删除路径本就先清历史，CASCADE 兜底防孤儿）
-- ============================================================

-- ---------- 1) 清理存量孤儿数据（先子后父，防止残留阻断 FK） ----------
DELETE h FROM rx_job_schedule_history h
LEFT JOIN rx_job_schedule s ON h.schedule_id = s.id
WHERE s.id IS NULL;

DELETE j FROM rx_job_schedule j
LEFT JOIN rx_ibmi_system i ON j.server_id = i.id
WHERE i.id IS NULL;

UPDATE rx_user u
LEFT JOIN rx_ibmi_system i ON u.as400_server_id = i.id
SET u.as400_server_id = NULL
WHERE u.as400_server_id IS NOT NULL AND i.id IS NULL;

DELETE r FROM rx_alert_rule r
LEFT JOIN rx_ibmi_system i ON r.server_id = i.id
WHERE r.server_id IS NOT NULL AND i.id IS NULL;

DELETE h FROM rx_report_schedule_history h
LEFT JOIN rx_report_schedule s ON h.schedule_id = s.id
WHERE s.id IS NULL;

DELETE r FROM rx_report_schedule r
LEFT JOIN rx_ibmi_system i ON r.server_id = i.id
WHERE r.server_id IS NOT NULL AND i.id IS NULL;

-- ---------- 2) 外键约束 ----------

-- 作业调度 → 服务器（级联：服务器删除即清理其调度）
ALTER TABLE rx_job_schedule
    ADD CONSTRAINT fk_job_schedule_server
        FOREIGN KEY (server_id) REFERENCES rx_ibmi_system (id) ON DELETE CASCADE;

-- 调度执行历史 → 调度（级联兜底，应用删除路径已先清历史）
ALTER TABLE rx_job_schedule_history
    ADD CONSTRAINT fk_job_schedule_history_schedule
        FOREIGN KEY (schedule_id) REFERENCES rx_job_schedule (id) ON DELETE CASCADE;

-- 用户 AS400 登录来源 → 服务器（置空：不因服务器删除而删除用户）
ALTER TABLE rx_user
    ADD CONSTRAINT fk_user_as400_server
        FOREIGN KEY (as400_server_id) REFERENCES rx_ibmi_system (id) ON DELETE SET NULL;

-- 告警规则 → 服务器（级联：服务器删除即清理其专属规则；server_id NULL=全局规则不受影响）
ALTER TABLE rx_alert_rule
    ADD CONSTRAINT fk_alert_rule_server
        FOREIGN KEY (server_id) REFERENCES rx_ibmi_system (id) ON DELETE CASCADE;

-- 报表定时任务 → 服务器（级联）
ALTER TABLE rx_report_schedule
    ADD CONSTRAINT fk_report_schedule_server
        FOREIGN KEY (server_id) REFERENCES rx_ibmi_system (id) ON DELETE CASCADE;

-- 报表执行历史 → 任务（级联兜底）
ALTER TABLE rx_report_schedule_history
    ADD CONSTRAINT fk_report_schedule_history_schedule
        FOREIGN KEY (schedule_id) REFERENCES rx_report_schedule (id) ON DELETE CASCADE;

-- ---------- 3) baseline 指标名列宽与 rx_metric.metric_name 对齐 ----------
ALTER TABLE rx_metric_baseline
    MODIFY COLUMN metric_name VARCHAR(100) NOT NULL;
