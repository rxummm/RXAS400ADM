-- V91: 修复 V70/V79 中 menu_name = 'AS400管理' 与 V66 中 'AS400 运维' 不匹配的问题
-- V70/V79 的子查询引用 'AS400管理' 但 V66 创建的是 'AS400 运维'，导致 parent_id = NULL，菜单成为孤儿

-- 1. 确保 'AS400管理' 菜单存在（与 'AS400 运维' 同级，挂在 groupAs400 下）
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, created_time, updated_time)
SELECT 'AS400管理', 1, 'as400ManagementGroup', '', '', 'fa-solid fa-server', 3,
    (SELECT id FROM rx_menu WHERE title = 'groupAs400' AND menu_type = 1 LIMIT 1),
    1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = 'AS400管理' AND menu_type = 1
);

-- 2. 将 V70 创建的孤儿菜单挂到正确的父级
UPDATE rx_menu m
INNER JOIN rx_menu p ON p.menu_name = 'AS400管理' AND p.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name = '用户Profile管理' AND m.menu_type = 1 AND (m.parent_id IS NULL OR m.parent_id = 0);

UPDATE rx_menu m
INNER JOIN rx_menu p ON p.menu_name = 'AS400管理' AND p.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name = '用户Profile列表' AND m.menu_type = 2 AND (m.parent_id IS NULL OR m.parent_id = 0);

-- 3. 将 V79 创建的孤儿菜单挂到正确的父级
UPDATE rx_menu m
INNER JOIN rx_menu p ON p.menu_name = 'AS400管理' AND p.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name = '系统健康仪表板' AND m.menu_type = 2 AND (m.parent_id IS NULL OR m.parent_id = 0);

UPDATE rx_menu m
INNER JOIN rx_menu p ON p.menu_name = 'AS400管理' AND p.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name = '备份监控' AND m.menu_type = 2 AND (m.parent_id IS NULL OR m.parent_id = 0);

UPDATE rx_menu m
INNER JOIN rx_menu p ON p.menu_name = 'AS400管理' AND p.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name = '系统值合规检查' AND m.menu_type = 2 AND (m.parent_id IS NULL OR m.parent_id = 0);

-- 4. 修复 V80：将控制塔/CPFR/TMS 从叶子菜单父级移到正确的 BPCS 父级
-- V80 错误地使用 path='/bpcs-forecast'（叶子菜单）做 parent_id，应改为 title='bpcs'（目录菜单）
-- 使用多表 UPDATE 避免 MySQL 1093 错误
UPDATE rx_menu m
INNER JOIN rx_menu p ON p.title = 'bpcs' AND p.menu_type = 1
INNER JOIN rx_menu bad ON bad.path IN ('/bpcs-forecast', '/bpcs-shipping') AND bad.menu_type = 1
SET m.parent_id = p.id, m.updated_time = NOW()
WHERE m.menu_name IN ('bpcsControlTower', 'bpcsCpfr', 'bpcsTms')
  AND m.parent_id = bad.id;
