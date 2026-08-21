-- ============================================================
-- V28: 建议后续功能 + 消息文件（*MSGF）查询
--  1) rx_job_sla 作业 SLA 规则表
--  2) 权限码：MSGF_VIEW / MSGF_ADD / MSGF_EDIT / MSGF_DELETE / SYSVAL_VIEW / SYSVAL_EDIT / SLA_MANAGE
--  3) 菜单：数据与对象·消息文件(+3 按钮) / 系统值(+1 按钮)；监控中心·服务器对比 / 巡检报告；
--     作业与任务·作业 SLA(+1 按钮) / 作业依赖图
--  4) 角色授权：页面→OPERATOR/DEVELOPER/VIEWER；按钮→OPERATOR/DEVELOPER
--  5) permission.menuDomain 追加新页面映射
-- ============================================================

-- 1) 作业 SLA 规则表
CREATE TABLE IF NOT EXISTS rx_job_sla (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_name             VARCHAR(64)  NOT NULL COMMENT '作业名',
    schedule_name        VARCHAR(128) NULL COMMENT '作业说明/调度名',
    expected_duration_sec INT         NOT NULL COMMENT '预期耗时（秒）',
    deviation_percent    INT          NOT NULL DEFAULT 20 COMMENT '允许偏差百分比',
    enabled              TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_time         DATETIME     NULL,
    updated_time         DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_job_sla_job (job_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作业 SLA 规则';

-- 2) 权限码（幂等）
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'MSGF_VIEW', '消息文件查看', 'MSGF', '消息文件与消息描述查询'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'MSGF_VIEW');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'MSGF_ADD', '新增消息描述', 'MSGF', '消息文件内新增消息描述（ADDMSGD）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'MSGF_ADD');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'MSGF_EDIT', '修改消息描述', 'MSGF', '消息文件内修改消息描述（CHGMSGD）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'MSGF_EDIT');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'MSGF_DELETE', '删除消息描述', 'MSGF', '消息文件内删除消息描述（RMVMSGD）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'MSGF_DELETE');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'SYSVAL_VIEW', '系统值查看', 'SYSVAL', 'AS400 系统值列表查询'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'SYSVAL_VIEW');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'SYSVAL_EDIT', '修改系统值', 'SYSVAL', '修改 AS400 系统值（CHGSYSVAL）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'SYSVAL_EDIT');
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'SLA_MANAGE', '作业SLA管理', 'SLA', '作业 SLA 规则的增删改'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'SLA_MANAGE');

-- 3a) 数据与对象：消息文件页（查询 message file 的 MESSAGE_ID / 文本 / 描述）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '消息文件', 2, 'messageFiles', '/message-files', 'views/data/messageFiles/index.vue', 'MSGF_VIEW', 'fa-solid fa-envelope-open-text', 8, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupData' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'messageFiles' AND m.menu_type = 2);

-- 3b) 消息文件三个按钮（按钮级授权）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '新增消息', 3, 'msgfAdd', NULL, NULL, 'MSGF_ADD', 'fa-solid fa-plus', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'messageFiles' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'msgfAdd' AND m.menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '修改消息', 3, 'msgfEdit', NULL, NULL, 'MSGF_EDIT', 'fa-solid fa-pen', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'messageFiles' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'msgfEdit' AND m.menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '删除消息', 3, 'msgfDelete', NULL, NULL, 'MSGF_DELETE', 'fa-solid fa-trash', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'messageFiles' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'msgfDelete' AND m.menu_type = 3);

-- 3c) 数据与对象：系统值页 + 修改按钮
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '系统值', 2, 'sysvals', '/sysvals', 'views/data/sysvals/index.vue', 'SYSVAL_VIEW', 'fa-solid fa-gears', 9, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupData' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'sysvals' AND m.menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '修改系统值', 3, 'sysvalEdit', NULL, NULL, 'SYSVAL_EDIT', 'fa-solid fa-pen', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'sysvals' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'sysvalEdit' AND m.menu_type = 3);

-- 3d) 监控中心：服务器对比 / 巡检报告
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '服务器对比', 2, 'serverCompare', '/server-compare', 'views/monitor/serverCompare/index.vue', 'MONITOR_VIEW', 'fa-solid fa-scale-balanced', 5, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupMonitor' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'serverCompare' AND m.menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '巡检报告', 2, 'inspection', '/inspection', 'views/monitor/inspection/index.vue', 'MONITOR_VIEW', 'fa-solid fa-clipboard-check', 6, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupMonitor' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'inspection' AND m.menu_type = 2);

-- 3e) 作业与任务：作业 SLA（+管理按钮）/ 作业依赖图
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '作业SLA', 2, 'jobSla', '/job-sla', 'views/job/sla/index.vue', 'JOB_VIEW', 'fa-solid fa-stopwatch', 5, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupJob' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'jobSla' AND m.menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, 'SLA管理', 3, 'slaManage', NULL, NULL, 'SLA_MANAGE', 'fa-solid fa-gears', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'jobSla' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'slaManage' AND m.menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '作业依赖图', 2, 'jobDependency', '/job-dependency', 'views/job/dependency/index.vue', 'JOB_VIEW', 'fa-solid fa-diagram-project', 6, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupJob' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'jobDependency' AND m.menu_type = 2);

-- 4) 角色授权（幂等）：页面→OPERATOR/DEVELOPER/VIEWER；按钮→OPERATOR/DEVELOPER
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code IN ('OPERATOR', 'DEVELOPER', 'VIEWER')
  AND m.title IN ('messageFiles', 'sysvals', 'serverCompare', 'inspection', 'jobSla', 'jobDependency')
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code IN ('OPERATOR', 'DEVELOPER')
  AND m.title IN ('msgfAdd', 'msgfEdit', 'msgfDelete', 'slaManage')
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'OPERATOR'
  AND m.title = 'sysvalEdit'
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- 5) permission.menuDomain 追加映射（菜单管理 perms 下拉建议依据）
UPDATE rx_config
SET config_value = CONCAT(
        LEFT(config_value, LENGTH(config_value) - 1),
        ',"messageFiles":["MSGF"],"sysvals":["SYSVAL"],"serverCompare":["MONITOR"],"inspection":["MONITOR","INSPECT"],"jobSla":["SLA","JOB"],"jobDependency":["JOB"]}')
WHERE config_key = 'permission.menuDomain'
  AND config_value LIKE '%}'
  AND config_value NOT LIKE '%messageFiles%';
