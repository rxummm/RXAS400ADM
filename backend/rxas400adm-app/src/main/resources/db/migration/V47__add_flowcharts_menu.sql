-- W2：新增「模块流程图」顶级菜单（方案 A：iframe 内嵌 VitePress docs 站的流程图单页）。
-- 页面：frontend/src/views/flowcharts/index.vue（iframe 加载 VITE_DOCS_URL /flowcharts/modules-mermaid-source）。
-- 特性：admin_only=1 仅管理员可见（非管理员菜单查询自动排除，见 MenuService）；menu_type=2 叶子；
--       perms 留空（admin_only 菜单不参与权限码/角色授权）；sort=99 置于菜单末尾；status=1 启用。
-- 幂等说明：Flyway 每版本仅执行一次；WHERE NOT EXISTS 防止重复执行/已有同名菜单时跳过。
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '模块流程图', 2, 'flowcharts', '/flowcharts', 'views/flowcharts/index.vue', NULL, 'Connection', 99, 1, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'flowcharts' AND menu_type = 2);
