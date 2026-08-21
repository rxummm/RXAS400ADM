-- ============================================================
-- V9: 菜单分组（参照旧项目两级菜单结构）
-- 新增 6 个目录菜单（menu_type=1），将既有平铺菜单归入对应分组：
--   监控中心(2)  作业与任务(3)  数据与对象(4)  报表与文档(5)  系统管理(6)  发布与代码(90, 停用)
-- 幂等：目录按 title 去重；叶子按 title 更新 parent_id 与组内排序
-- ============================================================

-- ---- 目录菜单（INSERT ... SELECT WHERE NOT EXISTS，按 title 去重） ----
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '监控中心', 1, 'groupMonitor', '/group-monitor', NULL, NULL, 'Monitor', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupMonitor');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '作业与任务', 1, 'groupJob', '/group-job', NULL, NULL, 'Suitcase', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupJob');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '数据与对象', 1, 'groupData', '/group-data', NULL, NULL, 'Box', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupData');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '报表与文档', 1, 'groupReport', '/group-report', NULL, NULL, 'DataAnalysis', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupReport');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '系统管理', 1, 'system', '/system', NULL, NULL, 'Setting', 6, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'system');

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '发布与代码', 1, 'groupRelease', '/group-release', NULL, NULL, 'Promotion', 90, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupRelease');

-- ---- 叶子归组 + 组内排序（JOIN 自表避免 MySQL 同表子查询限制） ----
-- 监控中心
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupMonitor' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'monitor';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupMonitor' SET m.parent_id = p.id, m.sort = 2 WHERE m.title = 'health';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupMonitor' SET m.parent_id = p.id, m.sort = 3 WHERE m.title = 'assets';
-- 作业与任务
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupJob' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'jobs';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupJob' SET m.parent_id = p.id, m.sort = 2 WHERE m.title = 'schedules';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupJob' SET m.parent_id = p.id, m.sort = 3 WHERE m.title = 'scripts';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupJob' SET m.parent_id = p.id, m.sort = 4 WHERE m.title = 'executions';
-- 数据与对象
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'query';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 2 WHERE m.title = 'objects';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 3 WHERE m.title = 'ifs';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 4 WHERE m.title = 'subsystems';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 5 WHERE m.title = 'pf';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupData' SET m.parent_id = p.id, m.sort = 6 WHERE m.title = 'topology';
-- 报表与文档
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupReport' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'reports';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupReport' SET m.parent_id = p.id, m.sort = 2 WHERE m.title = 'docs';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupReport' SET m.parent_id = p.id, m.sort = 3 WHERE m.title = 'audit';
-- 系统管理
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'system' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'users';
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'system' SET m.parent_id = p.id, m.sort = 2 WHERE m.title = 'menus';
-- 发布与代码（停用组）
UPDATE rx_menu m JOIN rx_menu p ON p.title = 'groupRelease' SET m.parent_id = p.id, m.sort = 1 WHERE m.title = 'source';