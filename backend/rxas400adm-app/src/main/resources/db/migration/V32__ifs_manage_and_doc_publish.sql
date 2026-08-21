-- ============================================================
-- V32: IFS 文件管理（上传/下载/新建目录/删除）+ 文档发布路径落库
--  1) rx_doc 增加 ifs_path：最近一次「上传到 IFS」的目标路径
--  2) 权限码：IFS_MANAGE（IFS 页文件管理操作）
--  3) 按钮级菜单（menu_type=3）：ifsUpload / ifsDownload / ifsMkdir / ifsDelete
--     挂在 IFS 页（title='ifs'）下，管理员可单独授权
--  4) 角色授权：按钮→OPERATOR/DEVELOPER
-- ============================================================

-- 1) rx_doc 增加 ifs_path + deleted（逻辑删除标记：删除文档=标记不物理删，保留审批/版本历史）
--    MySQL 8 不支持 ADD COLUMN IF NOT EXISTS（MariaDB 语法），V32 仅执行一次，直接 ADD
ALTER TABLE rx_doc
    ADD COLUMN ifs_path VARCHAR(500) NULL COMMENT '最近一次上传到 IFS 的目标路径' AFTER reject_reason;
ALTER TABLE rx_doc
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=正常 1=已删除' AFTER ifs_path;

-- 2) 权限码（幂等）
INSERT INTO rx_permission (permission_code, permission_name, module, description)
SELECT 'IFS_MANAGE', 'IFS文件管理', 'IFS', 'IFS 文件上传/下载/新建目录/删除'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rx_permission WHERE permission_code = 'IFS_MANAGE');

-- 3) 按钮级菜单（挂在 IFS 页下）
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '上传文件', 3, 'ifsUpload', NULL, NULL, 'IFS_MANAGE', 'fa-solid fa-upload', 1, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'ifs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'ifsUpload' AND m.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '下载文件', 3, 'ifsDownload', NULL, NULL, 'IFS_MANAGE', 'fa-solid fa-download', 2, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'ifs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'ifsDownload' AND m.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '新建目录', 3, 'ifsMkdir', NULL, NULL, 'IFS_MANAGE', 'fa-solid fa-folder-plus', 3, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'ifs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'ifsMkdir' AND m.menu_type = 3);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT p.id, '删除文件', 3, 'ifsDelete', NULL, NULL, 'IFS_MANAGE', 'fa-solid fa-trash', 4, 1, 1, 0, NOW(), NOW()
FROM rx_menu p
WHERE p.title = 'ifs' AND p.menu_type = 2
  AND NOT EXISTS (SELECT 1 FROM rx_menu m WHERE m.title = 'ifsDelete' AND m.menu_type = 3);

-- 4) 角色授权（按钮→OPERATOR/DEVELOPER）
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code IN ('OPERATOR', 'DEVELOPER')
  AND m.title IN ('ifsUpload', 'ifsDownload', 'ifsMkdir', 'ifsDelete')
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
