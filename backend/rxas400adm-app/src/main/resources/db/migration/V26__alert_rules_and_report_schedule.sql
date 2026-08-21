-- ============================================================
-- V26: 告警规则可视化配置 + 报表定时生成/邮件推送 + 服务器管理 UI
--     1) rx_alert_rule 增加通道（channel）与服务器维度（server_id）
--        channel: ALL=Webhook+邮件 / WEBHOOK / EMAIL / NONE=仅站内
--        server_id: NULL=全部服务器（原有规则语义），否则仅该服务器
--     2) 报表定时任务 rx_report_schedule + 执行历史 rx_report_schedule_history
--     3) 权限码 ALERT_MANAGE / REPORT_MANAGE（菜单管理 perms 下拉按域过滤：
--        alertRules→[MONITOR,ALERT]，reports→[REPORT]）
--     4) 菜单：告警规则页（监控中心）、报表定时管理按钮、报表两 Tab（手动/定时）、
--        资产清单改名「服务器管理」
-- ============================================================

-- 1) 告警规则扩展
ALTER TABLE rx_alert_rule
    ADD COLUMN channel VARCHAR(40) NULL,
    ADD COLUMN server_id BIGINT NULL,
    ADD COLUMN description VARCHAR(255) NULL;

UPDATE rx_alert_rule SET channel = 'ALL' WHERE channel IS NULL;

-- 2) 报表定时任务
CREATE TABLE rx_report_schedule (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    report_type VARCHAR(20)  NOT NULL COMMENT 'metrics/executions/capacity',
    format      VARCHAR(10)  NOT NULL DEFAULT 'xlsx' COMMENT 'xlsx/pdf',
    server_id   BIGINT       NULL COMMENT '指标/容量报表目标服务器；执行记录报表不用',
    days        INT          NOT NULL DEFAULT 7,
    cron_expr   VARCHAR(100) NOT NULL,
    recipients  TEXT         NULL COMMENT '收件邮箱，逗号/分号/空格分隔',
    enabled     TINYINT(1)   DEFAULT 1,
    status      VARCHAR(20)  DEFAULT 'PENDING',
    last_run_time DATETIME   NULL,
    last_result VARCHAR(500) NULL,
    created_by  VARCHAR(50),
    created_time DATETIME,
    updated_time DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rx_report_schedule_history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT       NOT NULL,
    run_time    DATETIME     NOT NULL,
    status      VARCHAR(20),
    message     VARCHAR(1000),
    file_bytes  BIGINT       DEFAULT 0 COMMENT '附件大小（字节）',
    created_time DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_report_schedule_history ON rx_report_schedule_history (schedule_id, run_time);

-- 3) 权限码
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'ALERT_MANAGE', '告警规则管理', 'ALERT', '告警规则的增删改与启停'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'ALERT_MANAGE');

INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'REPORT_MANAGE', '报表定时任务管理', 'REPORT', '报表定时任务与邮件推送的增删改、启停、立即执行'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'REPORT_MANAGE');

-- 4a) 告警规则页（监控中心目录下，title=alertRules）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '告警规则', 2, 'alertRules', '/monitor/alert-rules', 'views/monitor/alertRules/index.vue', 'MONITOR_VIEW', 'fa-solid fa-bell', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupMonitor' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'alertRules' AND m.menu_type = 2);

-- 4b) 告警规则管理按钮（挂在告警规则页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '告警规则管理', 3, 'alertRuleManage', NULL, NULL, 'ALERT_MANAGE', 'fa-solid fa-pen-to-square', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'alertRules' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'alertRuleManage' AND m.menu_type = 3);

-- 4c) 报表定时管理按钮（挂在报表中心页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '定时报表管理', 3, 'reportScheduleManage', NULL, NULL, 'REPORT_MANAGE', 'fa-solid fa-clock', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'reports' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'reportScheduleManage' AND m.menu_type = 3);

-- 4d) 报表中心两个 Tab（menu_type=4，perms 空=默认可见，管理员可后续控制）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '手动导出', 4, 'reportManualTab', NULL, NULL, NULL, 'fa-solid fa-file-export', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'reports' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'reportManualTab' AND m.menu_type = 4);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '定时任务', 4, 'reportScheduleTab', NULL, NULL, NULL, 'fa-solid fa-calendar-check', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'reports' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'reportScheduleTab' AND m.menu_type = 4);

-- 4e) 资产清单页改名「服务器管理」（同一页面承载服务器 CRUD/命令，其他资产维度
--     已由 监控/对象/拓扑/IFS 等页体现）
UPDATE rx_menu
SET menu_name = '服务器管理', updated_time = NOW()
WHERE title = 'assets' AND menu_type = 2;
