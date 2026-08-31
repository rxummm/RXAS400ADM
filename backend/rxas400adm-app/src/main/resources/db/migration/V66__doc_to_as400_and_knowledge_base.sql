-- V66：文档管理迁入 AS400 运维分组 + 新建知识库（system 模块纯 DB 文档）
-- ============================================================

-- 1. 新建「AS400 运维」顶级目录（sort=2，紧跟监控中心）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, 'AS400 运维', 1, 'groupAs400', '/group-as400', 'Monitor', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'groupAs400' AND menu_type = 1);

-- 2. 将「文档管理」叶子从 groupReport 移入 groupAs400
UPDATE rx_menu m
JOIN (SELECT id FROM rx_menu WHERE title = 'groupAs400' AND menu_type = 1 LIMIT 1) AS new_parent
SET m.parent_id = new_parent.id, m.sort = 1, m.updated_time = NOW()
WHERE m.title = 'docs' AND m.menu_type = 2;

-- 3. 新建「知识库」叶子（system 模块，纯 DB 文档）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'system' AND menu_type = 1 LIMIT 1), '知识库', 2, 'sysDocs', '/sys-docs', 'views/sysDocs/index.vue', 'SYS_DOC_VIEW', 'Collection', 5, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'sysDocs' AND menu_type = 2);

-- 4. 新建知识库按钮菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'sysDocs' AND menu_type = 2 LIMIT 1), '知识库管理', 3, 'sysDocManage', 'SYS_DOC_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'sysDocManage' AND menu_type = 3);

-- 5. 新增权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
  ('SYS_DOC_VIEW', '知识库查看', 'SYSTEM'),
  ('SYS_DOC_MANAGE', '知识库管理', 'SYSTEM');

-- 6. ADMIN：全部权限（已通过 SELECT r.id, p.id 授予，无需额外操作）

-- 7. OPERATOR：新增 groupAs400 + sysDocs + SYS_DOC_MANAGE
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'OPERATOR'
  AND m.title IN ('groupAs400', 'sysDocs', 'sysDocManage')
  AND m.status = 1;

-- 8. DEVELOPER：新增 groupAs400 + sysDocs + SYS_DOC_MANAGE
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'DEVELOPER'
  AND m.title IN ('groupAs400', 'sysDocs', 'sysDocManage')
  AND m.status = 1;

-- 9. VIEWER：新增 groupAs400 + sysDocs（只读）
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'VIEWER'
  AND m.title IN ('groupAs400', 'sysDocs')
  AND m.status = 1;

-- 10. 新建知识库表（system 模块，纯 DB 元数据）
CREATE TABLE IF NOT EXISTS rx_sys_doc (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content MEDIUMTEXT COMMENT '正文（Markdown）',
  category VARCHAR(50) DEFAULT NULL COMMENT '分类',
  tags VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
  created_by VARCHAR(50) DEFAULT NULL,
  updated_by VARCHAR(50) DEFAULT NULL,
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sys_doc_status (status),
  INDEX idx_sys_doc_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档（纯 DB，无 IFS 存储）';
