-- ============================================================================
-- V38__seed_platform_structure.sql
-- M1：菜单/权限/角色双源收敛——平台基础结构（权限码/角色/菜单树/角色授权）全部下沉为
-- Flyway 幂等种子，DataInitializer 移除播种逻辑，仅保留演示数据（admin/示例服务器/告警规则）。
--
-- 幂等策略（在已应用的 dev/生产库重复执行无副作用）：
--   rx_role / rx_permission：依赖 UNIQUE 键 → INSERT IGNORE
--   rx_menu：无唯一键，逐行 INSERT ... SELECT ... WHERE NOT EXISTS（叶子/按钮按 title+type，
--     Tab 按 parent+title+type——不同页面允许同名 Tab，如 objects/topology 都有 refIn）
--   rx_role_menu / rx_role_permission：主键天然去重 → INSERT IGNORE
-- ============================================================================

-- ---------- 1. 角色（role_code UNIQUE） ----------
INSERT IGNORE INTO rx_role (role_code, role_name, description, sort, status) VALUES
('ADMIN', '系统管理员', NULL, 0, 1),
('OPERATOR', 'IBM i 运维员', NULL, 0, 1),
('DEVELOPER', '开发人员', NULL, 0, 1),
('VIEWER', '只读用户', NULL, 0, 1);

-- ---------- 2. 权限码（permission_code UNIQUE，module 与 DataInitializer.initPermissions 一致用 SYSTEM） ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('JOB_VIEW', 'JOB_VIEW', 'SYSTEM'),
('JOB_END', 'JOB_END', 'SYSTEM'),
('MONITOR_VIEW', 'MONITOR_VIEW', 'SYSTEM'),
('USER_MANAGE', 'USER_MANAGE', 'SYSTEM'),
('SOURCE_VIEW', 'SOURCE_VIEW', 'SYSTEM'),
('COMPILE_EXECUTE', 'COMPILE_EXECUTE', 'SYSTEM'),
('QUERY_EXECUTE', 'QUERY_EXECUTE', 'SYSTEM'),
('AS400_MANAGE', 'AS400_MANAGE', 'SYSTEM'),
('OBJECT_VIEW', 'OBJECT_VIEW', 'SYSTEM'),
('SCHEDULE_VIEW', 'SCHEDULE_VIEW', 'SYSTEM'),
('SCHEDULE_MANAGE', 'SCHEDULE_MANAGE', 'SYSTEM'),
('SCRIPT_VIEW', 'SCRIPT_VIEW', 'SYSTEM'),
('SCRIPT_MANAGE', 'SCRIPT_MANAGE', 'SYSTEM'),
('IFS_VIEW', 'IFS_VIEW', 'SYSTEM'),
('SUBSYSTEM_VIEW', 'SUBSYSTEM_VIEW', 'SYSTEM'),
('SUBSYSTEM_MANAGE', 'SUBSYSTEM_MANAGE', 'SYSTEM'),
('EXECUTION_VIEW', 'EXECUTION_VIEW', 'SYSTEM'),
('PF_VIEW', 'PF_VIEW', 'SYSTEM'),
('AUDIT_VIEW', 'AUDIT_VIEW', 'SYSTEM'),
('HEALTH_VIEW', 'HEALTH_VIEW', 'SYSTEM'),
('TOPOLOGY_VIEW', 'TOPOLOGY_VIEW', 'SYSTEM'),
('REPORT_VIEW', 'REPORT_VIEW', 'SYSTEM'),
('REPORT_MANAGE', 'REPORT_MANAGE', 'SYSTEM'),
('DOC_VIEW', 'DOC_VIEW', 'SYSTEM'),
('DOC_MANAGE', 'DOC_MANAGE', 'SYSTEM'),
('DOC_APPROVE', 'DOC_APPROVE', 'SYSTEM'),
('MENU_MANAGE', 'MENU_MANAGE', 'SYSTEM'),
('REGION_VIEW', 'REGION_VIEW', 'SYSTEM'),
('REGION_MANAGE', 'REGION_MANAGE', 'SYSTEM'),
('CALENDAR_VIEW', 'CALENDAR_VIEW', 'SYSTEM'),
('CALENDAR_MANAGE', 'CALENDAR_MANAGE', 'SYSTEM'),
('ROLE_MANAGE', 'ROLE_MANAGE', 'SYSTEM'),
('SYS_CONFIG_MANAGE', 'SYS_CONFIG_MANAGE', 'SYSTEM'),
('I18N_MANAGE', 'I18N_MANAGE', 'SYSTEM'),
('SYS_CACHE_MANAGE', 'SYS_CACHE_MANAGE', 'SYSTEM'),
('SYS_TASK_MANAGE', 'SYS_TASK_MANAGE', 'SYSTEM'),
('SYS_IP_MANAGE', 'SYS_IP_MANAGE', 'SYSTEM'),
('WEBHOOK_MANAGE', 'WEBHOOK_MANAGE', 'SYSTEM'),
('NOTICE_MANAGE', 'NOTICE_MANAGE', 'SYSTEM'),
('SYS_PERMISSION_REQUEST', 'SYS_PERMISSION_REQUEST', 'SYSTEM'),
('NOTIFICATION_MANAGE', 'NOTIFICATION_MANAGE', 'SYSTEM'),
('DICT_MANAGE', 'DICT_MANAGE', 'SYSTEM'),
('PERMISSION_MANAGE', 'PERMISSION_MANAGE', 'SYSTEM'),
('ALERT_MANAGE', 'ALERT_MANAGE', 'SYSTEM'),
('MSGF_VIEW', 'MSGF_VIEW', 'SYSTEM'),
('MSGF_ADD', 'MSGF_ADD', 'SYSTEM'),
('MSGF_EDIT', 'MSGF_EDIT', 'SYSTEM'),
('MSGF_DELETE', 'MSGF_DELETE', 'SYSTEM'),
('SYSVAL_VIEW', 'SYSVAL_VIEW', 'SYSTEM'),
('SYSVAL_EDIT', 'SYSVAL_EDIT', 'SYSTEM'),
('SLA_VIEW', 'SLA_VIEW', 'SYSTEM'),
('SLA_EDIT', 'SLA_EDIT', 'SYSTEM'),
('SLA_MANAGE', 'SLA_MANAGE', 'SYSTEM'),
('INSPECT_VIEW', 'INSPECT_VIEW', 'SYSTEM'),
('IFS_MANAGE', 'IFS_MANAGE', 'SYSTEM');

-- ---------- 3. 菜单树（menu_type：1=目录 2=叶子/页面 3=按钮 4=Tab） ----------

-- 3.1 目录（menu_type=1）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '监控中心', 1, 'groupMonitor', '/group-monitor', 'Monitor', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '作业与任务', 1, 'groupJob', '/group-job', 'Suitcase', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '数据与对象', 1, 'groupData', '/group-data', 'Box', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupData' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '报表与文档', 1, 'groupReport', '/group-report', 'DataAnalysis', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupReport' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '系统管理', 1, 'system', '/system', 'Setting', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'system' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '发布与代码', 1, 'groupRelease', '/group-release', 'Promotion', 90, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupRelease' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '工具', 1, 'groupTools', '/group-tools', 'Tools', 7, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupTools' AND menu_type = 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '业务管理', 1, 'biz', '/group-biz', 'Briefcase', 8, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'biz' AND menu_type = 1);

-- 3.2 顶级叶子（总览）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '总览', 2, 'dashboard', '/dashboard', 'views/Dashboard.vue', NULL, 'Odometer', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'dashboard' AND menu_type = 2);

-- 3.3 发布与代码（停用组 status=0，source 叶子必须先于「编译执行」按钮插入）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupRelease' AND menu_type = 1 LIMIT 1), '源代码', 2, 'source', '/source', 'views/Source.vue', NULL, 'FolderOpened', 1, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'source' AND menu_type = 2);

-- 3.4 监控中心叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '监控中心', 2, 'monitor', '/monitor', 'views/Monitor.vue', 'MONITOR_VIEW', 'Monitor', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'monitor' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '健康巡检', 2, 'health', '/health', 'views/health/index.vue', 'HEALTH_VIEW', 'FirstAidKit', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'health' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '服务器管理', 2, 'assets', '/assets', 'views/assets/index.vue', NULL, 'Coin', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'assets' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '告警规则', 2, 'alertRules', '/monitor/alert-rules', 'views/monitor/alertRules/index.vue', 'MONITOR_VIEW', 'fa-solid fa-bell', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'alertRules' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '服务器对比', 2, 'serverCompare', '/server-compare', 'views/monitor/serverCompare/index.vue', 'MONITOR_VIEW', 'fa-solid fa-scale-balanced', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'serverCompare' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupMonitor' AND menu_type = 1 LIMIT 1), '巡检报告', 2, 'inspection', '/inspection', 'views/monitor/inspection/index.vue', 'MONITOR_VIEW', 'fa-solid fa-clipboard-check', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'inspection' AND menu_type = 2);

-- 3.5 作业与任务叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), 'Job 中心', 2, 'jobs', '/jobs', 'views/job/index.vue', 'JOB_VIEW', 'Suitcase', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'jobs' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), '作业调度', 2, 'schedules', '/schedules', 'views/schedule/index.vue', 'SCHEDULE_VIEW', 'AlarmClock', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'schedules' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), '命令脚本', 2, 'scripts', '/scripts', 'views/scripts/index.vue', 'SCRIPT_VIEW', 'Files', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'scripts' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), '执行审计', 2, 'executions', '/executions', 'views/executions/index.vue', 'EXECUTION_VIEW', 'Document', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'executions' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), '作业SLA', 2, 'jobSla', '/job-sla', 'views/job/sla/index.vue', 'JOB_VIEW', 'fa-solid fa-stopwatch', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'jobSla' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupJob' AND menu_type = 1 LIMIT 1), '作业依赖图', 2, 'jobDependency', '/job-dependency', 'views/job/dependency/index.vue', 'JOB_VIEW', 'fa-solid fa-diagram-project', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'jobDependency' AND menu_type = 2);

-- 3.6 数据与对象叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '数据查询', 2, 'query', '/query', 'views/query/index.vue', 'QUERY_EXECUTE', 'Search', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'query' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '对象搜索', 2, 'objects', '/objects', 'views/objects/index.vue', 'OBJECT_VIEW', 'Box', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'objects' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), 'IFS 文件', 2, 'ifs', '/ifs', 'views/ifs/index.vue', 'IFS_VIEW', 'FolderOpened', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ifs' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '系统服务', 2, 'subsystems', '/subsystems', 'views/subsystems/index.vue', 'SUBSYSTEM_VIEW', 'Setting', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'subsystems' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), 'PF 文件浏览', 2, 'pf', '/pf', 'views/pf/index.vue', 'PF_VIEW', 'Tickets', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'pf' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '拓扑分析', 2, 'topology', '/topology', 'views/topology/index.vue', 'TOPOLOGY_VIEW', 'Share', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'topology' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '文件字段', 2, 'tableFields', '/table-fields', 'views/data/tableFields/index.vue', 'QUERY_EXECUTE', 'fa-solid fa-file-lines', 7, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tableFields' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '消息文件', 2, 'messageFiles', '/message-files', 'views/data/messageFiles/index.vue', 'MSGF_VIEW', 'fa-solid fa-envelope-open-text', 8, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'messageFiles' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupData' AND menu_type = 1 LIMIT 1), '系统值', 2, 'sysvals', '/sysvals', 'views/data/sysvals/index.vue', 'SYSVAL_VIEW', 'fa-solid fa-gears', 9, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'sysvals' AND menu_type = 2);

-- 3.7 业务管理叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'biz' AND menu_type = 1 LIMIT 1), '业务数据浏览', 2, 'bizData', '/biz-data', 'views/biz/data/index.vue', 'QUERY_EXECUTE', 'fa-solid fa-table', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bizData' AND menu_type = 2);

-- 3.8 报表与文档叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupReport' AND menu_type = 1 LIMIT 1), '报表中心', 2, 'reports', '/reports', 'views/report/index.vue', 'REPORT_VIEW', 'DataAnalysis', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'reports' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupReport' AND menu_type = 1 LIMIT 1), '文档管理', 2, 'docs', '/docs', 'views/docs/index.vue', 'DOC_VIEW', 'Collection', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'docs' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupReport' AND menu_type = 1 LIMIT 1), '审计日志', 2, 'audit', '/audit', 'views/audit/index.vue', 'AUDIT_VIEW', 'Notebook', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'audit' AND menu_type = 2);

-- 3.9 系统管理叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '用户管理', 2, 'users', '/users', 'views/system/Users.vue', 'USER_MANAGE', 'User', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'users' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '菜单管理', 2, 'menus', '/menus', 'views/system/menus/index.vue', 'MENU_MANAGE', 'Menu', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'menus' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '系统配置', 2, 'config', '/system/config', 'views/system/config/index.vue', 'SYS_CONFIG_MANAGE', 'fa-solid fa-sliders', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'config' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '翻译管理', 2, 'i18n', '/system/i18n', 'views/system/i18n/index.vue', 'I18N_MANAGE', 'fa-solid fa-globe', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'i18n' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '登录日志', 2, 'loginLog', '/system/login-log', 'views/system/loginLog/index.vue', 'AUDIT_VIEW', 'fa-solid fa-clock-rotate-left', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'loginLog' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '缓存管理', 2, 'cache', '/system/cache', 'views/system/cache/index.vue', 'SYS_CACHE_MANAGE', 'fa-solid fa-bolt', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'cache' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '定时任务', 2, 'tasks', '/system/tasks', 'views/system/tasks/index.vue', 'SYS_TASK_MANAGE', 'fa-solid fa-clock', 7, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tasks' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), 'IP 黑白名单', 2, 'ipRules', '/system/ip-rules', 'views/system/ipRules/index.vue', 'SYS_IP_MANAGE', 'fa-solid fa-shield', 8, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ipRules' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), 'Webhook 管理', 2, 'webhooks', '/system/webhooks', 'views/system/webhooks/index.vue', 'WEBHOOK_MANAGE', 'fa-solid fa-envelope', 9, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'webhooks' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '权限申请', 2, 'permissionRequest', '/system/permission-request', 'views/system/permissionRequest/index.vue', 'SYS_PERMISSION_REQUEST', 'fa-solid fa-key', 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'permissionRequest' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '通知公告', 2, 'notices', '/system/notices', 'views/system/notice/index.vue', 'NOTICE_MANAGE', 'fa-solid fa-bullhorn', 11, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'notices' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '字典管理', 2, 'dict', '/system/dict', 'views/system/dict/index.vue', 'DICT_MANAGE', 'fa-solid fa-book', 12, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'dict' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '权限码管理', 2, 'permissions', '/system/permissions', 'views/system/permissions/index.vue', 'PERMISSION_MANAGE', 'fa-solid fa-key', 13, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'permissions' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '参数维护', 2, 'params', '/system/params', 'views/system/params/index.vue', NULL, 'fa-solid fa-gears', 14, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'params' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '角色管理', 2, 'roles', '/roles', 'views/system/roles/index.vue', 'ROLE_MANAGE', 'UserFilled', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'roles' AND menu_type = 2);

-- 3.10 工具叶子
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupTools' AND menu_type = 1 LIMIT 1), '区域管理', 2, 'region', '/region', 'views/tool/region/index.vue', 'REGION_VIEW', 'Location', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'region' AND menu_type = 2);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'groupTools' AND menu_type = 1 LIMIT 1), '日历', 2, 'calendar', '/calendar', 'views/calendar/index.vue', 'CALENDAR_VIEW', 'Calendar', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'calendar' AND menu_type = 2);

-- 3.11 按钮（menu_type=3，父为对应页面叶子）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobSla' AND menu_type = 2 LIMIT 1), 'SLA管理', 3, 'slaManage', 'SLA_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'slaManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'messageFiles' AND menu_type = 2 LIMIT 1), '新增消息', 3, 'msgfAdd', 'MSGF_ADD', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'msgfAdd' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'messageFiles' AND menu_type = 2 LIMIT 1), '修改消息', 3, 'msgfEdit', 'MSGF_EDIT', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'msgfEdit' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'messageFiles' AND menu_type = 2 LIMIT 1), '删除消息', 3, 'msgfDelete', 'MSGF_DELETE', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'msgfDelete' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'sysvals' AND menu_type = 2 LIMIT 1), '修改系统值', 3, 'sysvalEdit', 'SYSVAL_EDIT', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'sysvalEdit' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'alertRules' AND menu_type = 2 LIMIT 1), '告警规则管理', 3, 'alertRuleManage', 'ALERT_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'alertRuleManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'reports' AND menu_type = 2 LIMIT 1), '定时报表管理', 3, 'reportScheduleManage', 'REPORT_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'reportScheduleManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1), '作业控制', 3, 'jobControl', 'JOB_END', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'jobControl' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1), '消息应答', 3, 'jobReply', 'JOB_END', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'jobReply' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'subsystems' AND menu_type = 2 LIMIT 1), '启停子系统', 3, 'subsystemControl', 'SUBSYSTEM_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'subsystemControl' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'scripts' AND menu_type = 2 LIMIT 1), '脚本管理', 3, 'scriptManage', 'SCRIPT_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'scriptManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'schedules' AND menu_type = 2 LIMIT 1), '调度管理', 3, 'scheduleManage', 'SCHEDULE_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'scheduleManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'docs' AND menu_type = 2 LIMIT 1), '文档管理', 3, 'docManage', 'DOC_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'docManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'docs' AND menu_type = 2 LIMIT 1), '文档审批', 3, 'docApprove', 'DOC_APPROVE', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'docApprove' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'calendar' AND menu_type = 2 LIMIT 1), '日历管理', 3, 'calendarManage', 'CALENDAR_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'calendarManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'region' AND menu_type = 2 LIMIT 1), '区域管理', 3, 'regionManage', 'REGION_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'regionManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'ifs' AND menu_type = 2 LIMIT 1), '上传文件', 3, 'ifsUpload', 'IFS_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ifsUpload' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'ifs' AND menu_type = 2 LIMIT 1), '下载文件', 3, 'ifsDownload', 'IFS_MANAGE', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ifsDownload' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'ifs' AND menu_type = 2 LIMIT 1), '新建目录', 3, 'ifsMkdir', 'IFS_MANAGE', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ifsMkdir' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'ifs' AND menu_type = 2 LIMIT 1), '删除文件', 3, 'ifsDelete', 'IFS_MANAGE', NULL, 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ifsDelete' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'source' AND menu_type = 2 LIMIT 1), '编译执行', 3, 'compileExecute', 'COMPILE_EXECUTE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'compileExecute' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'assets' AND menu_type = 2 LIMIT 1), '服务器管理', 3, 'assetsManage', 'AS400_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'assetsManage' AND menu_type = 3);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '通知管理', 3, 'notificationManage', 'NOTIFICATION_MANAGE', 'fa-solid fa-bell', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'notificationManage' AND menu_type = 3);

-- 3.12 Tab（menu_type=4，按 parent+title+type 判重，不同页面允许同名）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'permissionRequest' AND menu_type = 2 LIMIT 1), '审批管理', 4, 'permissionRequestReview', 'SYS_PERMISSION_REQUEST', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'permissionRequest' AND menu_type = 2 LIMIT 1) AND title = 'permissionRequestReview' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1), '作业', 4, 'jobsTab', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1) AND title = 'jobsTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1), '队列', 4, 'queuesTab', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1) AND title = 'queuesTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1), '输出', 4, 'spoolTab', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'jobs' AND menu_type = 2 LIMIT 1) AND title = 'spoolTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'users' AND menu_type = 2 LIMIT 1), '用户', 4, 'usersTab', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'users' AND menu_type = 2 LIMIT 1) AND title = 'usersTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'users' AND menu_type = 2 LIMIT 1), '登录安全', 4, 'securityTab', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'users' AND menu_type = 2 LIMIT 1) AND title = 'securityTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'webhooks' AND menu_type = 2 LIMIT 1), '配置', 4, 'configTab', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'webhooks' AND menu_type = 2 LIMIT 1) AND title = 'configTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'webhooks' AND menu_type = 2 LIMIT 1), '日志', 4, 'logTab', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'webhooks' AND menu_type = 2 LIMIT 1) AND title = 'logTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1), '引用入', 4, 'refIn', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1) AND title = 'refIn' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1), '引用出', 4, 'refOut', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1) AND title = 'refOut' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1), '对象权限', 4, 'authorities', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'objects' AND menu_type = 2 LIMIT 1) AND title = 'authorities' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'topology' AND menu_type = 2 LIMIT 1), '引用入', 4, 'refIn', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'topology' AND menu_type = 2 LIMIT 1) AND title = 'refIn' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'topology' AND menu_type = 2 LIMIT 1), '引用出', 4, 'refOut', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'topology' AND menu_type = 2 LIMIT 1) AND title = 'refOut' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'reports' AND menu_type = 2 LIMIT 1), '手动导出', 4, 'reportManualTab', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'reports' AND menu_type = 2 LIMIT 1) AND title = 'reportManualTab' AND menu_type = 4);
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'reports' AND menu_type = 2 LIMIT 1), '定时任务', 4, 'reportScheduleTab', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE parent_id = (SELECT id FROM rx_menu WHERE title = 'reports' AND menu_type = 2 LIMIT 1) AND title = 'reportScheduleTab' AND menu_type = 4);

-- ---------- 4. 角色授权 ----------

-- 4.1 ADMIN：全部权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p WHERE r.role_code = 'ADMIN';

-- 4.2 角色-菜单（与 DataInitializer.initRoleMenus 的标题集合一致；status=0 停用菜单不授予）
-- ADMIN：全部启用菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.status = 1;

-- OPERATOR（IBM i 运维员）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'OPERATOR' AND m.status = 1 AND m.title IN (
    'dashboard',
    'groupMonitor', 'monitor', 'health', 'assets',
    'groupJob', 'jobs', 'schedules', 'scripts', 'executions',
    'groupData', 'query', 'objects', 'ifs', 'subsystems', 'pf', 'topology',
    'groupReport', 'reports', 'docs', 'audit',
    'groupTools', 'region', 'calendar',
    'loginLog', 'permissionRequest',
    'biz', 'bizData', 'tableFields',
    'jobControl', 'jobReply', 'subsystemControl', 'scriptManage', 'scheduleManage',
    'messageFiles', 'sysvals', 'serverCompare', 'inspection', 'jobSla', 'jobDependency',
    'msgfAdd', 'msgfEdit', 'msgfDelete', 'sysvalEdit', 'slaManage'
);

-- DEVELOPER（开发人员）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'DEVELOPER' AND m.status = 1 AND m.title IN (
    'dashboard',
    'groupJob', 'schedules', 'scripts',
    'groupData', 'query', 'objects', 'pf',
    'groupReport', 'reports', 'docs',
    'groupTools', 'region', 'calendar',
    'permissionRequest',
    'biz', 'bizData', 'tableFields',
    'scriptManage', 'scheduleManage',
    'messageFiles', 'sysvals', 'serverCompare', 'inspection', 'jobSla', 'jobDependency',
    'msgfAdd', 'msgfEdit', 'msgfDelete', 'slaManage'
);

-- VIEWER（只读用户）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'VIEWER' AND m.status = 1 AND m.title IN (
    'dashboard',
    'groupMonitor', 'monitor', 'health', 'assets',
    'groupJob', 'jobs', 'schedules', 'scripts', 'executions',
    'groupData', 'query', 'objects', 'ifs', 'subsystems', 'pf', 'topology',
    'groupReport', 'reports', 'docs', 'audit',
    'groupTools', 'region', 'calendar',
    'biz', 'bizData', 'tableFields',
    'permissionRequest',
    'messageFiles', 'sysvals', 'serverCompare', 'inspection', 'jobSla', 'jobDependency'
);
