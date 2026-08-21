-- ============================================================
-- V25: 参数维护聚合页 + 服务器管理按钮
--     1) 系统管理下新增「参数维护」聚合页（title=params，perms 空，
--        内部 Tab 由各自子页权限码控制：系统参数/字典/权限码）
--     2) 资产清单下新增「服务器管理」按钮（menu_type=3，perms=AS400_MANAGE）
--        ——服务器增删改/命令接口（/as400/systems CRUD + command）挂该码，
--           角色授权后可见操作，未授权 403（11.14 加固缺口）
-- ============================================================

-- 参数维护聚合页（挂在「系统管理」目录下，sort=14 在权限码管理之后）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '参数维护', 2, 'params', '/system/params', 'views/system/params/index.vue', NULL, 'fa-solid fa-gears', 14, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'system' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'params' AND m.menu_type = 2);

-- 服务器管理按钮（挂在「资产清单」页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '服务器管理', 3, 'assetsManage', NULL, NULL, 'AS400_MANAGE', 'fa-solid fa-server', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'assets' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'assetsManage' AND m.menu_type = 3);

-- 权限码下拉建议的页面→模块映射配置（菜单管理 perms 下拉按业务域过滤的依据）
INSERT INTO rx_config (config_key, config_value, description)
SELECT 'permission.menuDomain',
       '{"jobs":["JOB"],"schedules":["SCHEDULE"],"scripts":["SCRIPT"],"executions":["EXECUTION"],"monitor":["MONITOR"],"health":["HEALTH"],"query":["QUERY"],"objects":["OBJECT"],"ifs":["IFS"],"subsystems":["SUBSYSTEM"],"pf":["PF"],"topology":["TOPOLOGY"],"reports":["REPORT"],"docs":["DOC"],"audit":["AUDIT"],"loginLog":["AUDIT"],"users":["USER"],"roles":["ROLE"],"menus":["MENU"],"dict":["DICT"],"notices":["NOTICE"],"notifications":["NOTIFICATION"],"webhooks":["WEBHOOK"],"config":["SYS_CONFIG"],"cache":["SYS_CACHE"],"tasks":["SYS_TASK"],"ipRules":["SYS_IP"],"permissionRequest":["SYS_PERMISSION"],"permissions":["PERMISSION"],"i18n":["I18N"],"region":["REGION"],"calendar":["CALENDAR"],"source":["SOURCE","COMPILE"],"assets":["AS400"]}',
       '菜单 perms 下拉建议的业务域映射（页面 title → 权限码模块前缀，JSON，可在系统配置页维护）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'permission.menuDomain');