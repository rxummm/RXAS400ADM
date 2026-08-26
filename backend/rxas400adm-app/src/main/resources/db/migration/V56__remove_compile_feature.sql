-- V56：编译功能整体下线（产品决策，2026-08-25）
-- 删除 V1 建立的 rx_compile_record 表、V38 种子的 COMPILE_EXECUTE 权限与
-- source 页「编译执行」按钮（title=compileExecute, menu_type=3）及其角色关联；
-- permission.menuDomain 配置去除 COMPILE 域。历史迁移不可改，故以本迁移反向清理。
-- 幂等：全部使用 IF EXISTS / 条件 DELETE / REPLACE，可重复执行。

DELETE rm FROM rx_role_menu rm
JOIN rx_menu m ON m.id = rm.menu_id
WHERE m.title = 'compileExecute' AND m.menu_type = 3;

DELETE FROM rx_menu WHERE title = 'compileExecute' AND menu_type = 3;

DELETE rp FROM rx_role_permission rp
JOIN rx_permission p ON p.id = rp.permission_id
WHERE p.permission_code = 'COMPILE_EXECUTE';

DELETE FROM rx_permission WHERE permission_code = 'COMPILE_EXECUTE';

DROP TABLE IF EXISTS rx_compile_record;

UPDATE rx_config
SET config_value = REPLACE(config_value, '["SOURCE","COMPILE"]', '["SOURCE"]')
WHERE config_key = 'permission.menuDomain'
  AND config_value LIKE '%["SOURCE","COMPILE"]%';
