-- ============================================================
-- V27: 业务管理（通用业务数据浏览）+ 数据与对象·文件字段
--     借鉴旧项目 biz/order·sku·price（硬编码特定表）与 schemaCrud，
--     通用化改造：任意 库.表 → 字段定义（QSYS2.SYSCOLUMNS 含描述）+ 分页数据 + 关键词模糊查询。
--     1) 新建顶级目录「业务管理」（groupBiz，sort=8）+ 业务数据浏览页（QUERY_EXECUTE）
--     2) 数据与对象目录下新增「文件字段」页（QUERY_EXECUTE，查询文件字段长度/类型/描述）
--     3) OPERATOR / DEVELOPER / VIEWER 三个角色授权新菜单（ADMIN 走全量启用菜单逻辑）
--     4) permission.menuDomain 追加 bizData/tableFields 映射（菜单 perms 下拉建议用）
-- ============================================================

-- 1a) 业务管理目录
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '业务管理', 1, 'biz', '/group-biz', NULL, NULL, 'fa-solid fa-briefcase', 8, 1, 1, 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'biz' AND menu_type = 1);

-- 1b) 业务数据浏览页（通用：选库选表 → 分页数据 + 关键词模糊查询，覆盖旧项目订单/SKU/价格三个页面）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '业务数据浏览', 2, 'bizData', '/biz-data', 'views/biz/data/index.vue', 'QUERY_EXECUTE', 'fa-solid fa-table', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'biz' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'bizData' AND m.menu_type = 2);

-- 2) 文件字段页（数据与对象目录下：查询文件的字段长度/类型/小数位/可空/描述）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '文件字段', 2, 'tableFields', '/table-fields', 'views/data/tableFields/index.vue', 'QUERY_EXECUTE', 'fa-solid fa-file-lines', 7, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'groupData' AND p.menu_type = 1
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'tableFields' AND m.menu_type = 2);

-- 3) 角色授权（OPERATOR / DEVELOPER / VIEWER 均授：业务目录 + 业务数据浏览 + 文件字段；幂等）
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code IN ('OPERATOR', 'DEVELOPER', 'VIEWER')
  AND m.title IN ('biz', 'bizData', 'tableFields')
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- 4) permission.menuDomain 追加映射（菜单管理 perms 下拉按业务域过滤的依据）
UPDATE rx_config
SET config_value = CONCAT(
        LEFT(config_value, LENGTH(config_value) - 1),
        ',"bizData":["QUERY"],"tableFields":["QUERY","PF"]}')
WHERE config_key = 'permission.menuDomain'
  AND config_value LIKE '%}'
  AND config_value NOT LIKE '%bizData%';
