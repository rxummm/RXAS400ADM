-- E1 告警升级去重：标记告警是否已发送过升级通知，防止定时任务重复轰炸
ALTER TABLE rx_alert_event
    ADD COLUMN upgrade_notified SMALLINT NOT NULL DEFAULT 0 COMMENT '升级通知已发送标记 0=未发送 1=已发送';

CREATE INDEX idx_alert_event_upgrade ON rx_alert_event (status, upgrade_notified, created_time);
