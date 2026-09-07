-- A4: 备份状态监控
CREATE TABLE IF NOT EXISTS rx_backup_status (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  server_id BIGINT NOT NULL COMMENT '服务器ID',
  backup_name VARCHAR(100) NOT NULL COMMENT '备份任务名称',
  backup_type VARCHAR(50) NOT NULL COMMENT '备份类型(FULL/INCREMENTAL/DIFFERENTIAL)',
  status VARCHAR(20) NOT NULL COMMENT '状态(RUNNING/SUCCESS/FAILED/WARNING)',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  duration_seconds BIGINT COMMENT '持续时间(秒)',
  objects_count INT COMMENT '对象数量',
  size_bytes BIGINT COMMENT '备份大小(字节)',
  media_name VARCHAR(100) COMMENT '介质名称',
  error_message TEXT COMMENT '错误信息',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_server_id (server_id),
  INDEX idx_status (status),
  INDEX idx_backup_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IBM i备份状态记录';

-- A8: 系统值合规检查
CREATE TABLE IF NOT EXISTS rx_system_value_compliance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  server_id BIGINT NOT NULL COMMENT '服务器ID',
  system_value VARCHAR(50) NOT NULL COMMENT '系统值名称',
  current_value VARCHAR(200) NOT NULL COMMENT '当前值',
  expected_value VARCHAR(200) NOT NULL COMMENT '期望值',
  compliance_status VARCHAR(20) NOT NULL COMMENT '合规状态(PASS/FAIL/WARNING)',
  severity VARCHAR(20) NOT NULL COMMENT '严重程度(CRITICAL/HIGH/MEDIUM/LOW)',
  description TEXT COMMENT '描述',
  remediation TEXT COMMENT '修复建议',
  last_checked DATETIME NOT NULL COMMENT '最后检查时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_server_id (server_id),
  INDEX idx_compliance_status (compliance_status),
  INDEX idx_severity (severity),
  UNIQUE KEY uk_server_sysval (server_id, system_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IBM i系统值合规检查';

-- 注册权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
    ('SYSTEM_HEALTH_VIEW', '查看系统健康仪表板', 'SYSTEM'),
    ('BACKUP_VIEW', '查看备份状态', 'SYSTEM'),
    ('BACKUP_MANAGE', '管理备份任务', 'SYSTEM'),
    ('COMPLIANCE_VIEW', '查看系统值合规', 'SYSTEM'),
    ('COMPLIANCE_MANAGE', '管理系统值合规', 'SYSTEM');

-- 为ADMIN角色分配权限
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('SYSTEM_HEALTH_VIEW', 'BACKUP_VIEW', 'BACKUP_MANAGE', 'COMPLIANCE_VIEW', 'COMPLIANCE_MANAGE');

-- 添加菜单：系统健康仪表板（挂在 AS400管理 目录下）
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, perms, created_time, updated_time)
SELECT '系统健康仪表板', 2, 'systemHealth', '/as400/system-health', 'views/as400/systemHealth/index.vue', 'fa-solid fa-heartbeat', 1,
    (SELECT id FROM (SELECT id FROM rx_menu WHERE menu_name = 'AS400管理' AND menu_type = 1 LIMIT 1) AS tmp),
    1, 1, 0, 'SYSTEM_HEALTH_VIEW', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = '系统健康仪表板' AND menu_type = 2
);

-- 添加菜单：备份监控（挂在 AS400管理 目录下）
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, perms, created_time, updated_time)
SELECT '备份监控', 2, 'backupMonitor', '/as400/backup-monitor', 'views/as400/backupMonitor/index.vue', 'fa-solid fa-database', 2,
    (SELECT id FROM (SELECT id FROM rx_menu WHERE menu_name = 'AS400管理' AND menu_type = 1 LIMIT 1) AS tmp),
    1, 1, 0, 'BACKUP_VIEW', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = '备份监控' AND menu_type = 2
);

-- 添加菜单：系统值合规检查（挂在 AS400管理 目录下）
INSERT IGNORE INTO rx_menu (menu_name, menu_type, title, path, component, icon, sort, parent_id, visible, status, admin_only, perms, created_time, updated_time)
SELECT '系统值合规检查', 2, 'systemValueCompliance', '/as400/system-value-compliance', 'views/as400/systemValueCompliance/index.vue', 'fa-solid fa-shield-halved', 3,
    (SELECT id FROM (SELECT id FROM rx_menu WHERE menu_name = 'AS400管理' AND menu_type = 1 LIMIT 1) AS tmp),
    1, 1, 0, 'COMPLIANCE_VIEW', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM rx_menu WHERE menu_name = '系统值合规检查' AND menu_type = 2
);

-- 为ADMIN角色分配菜单
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN'
  AND m.menu_name IN ('系统健康仪表板', '备份监控', '系统值合规检查');
