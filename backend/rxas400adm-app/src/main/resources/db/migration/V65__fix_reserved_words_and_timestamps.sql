-- ============================================================
-- V65: 修复保留字列名 + 补全时间戳默认值
-- 1. rx_ibmi_system.name -> system_name (MySQL 保留字)
-- 2. 为缺少默认值的 created_time/updated_time 列添加 DEFAULT + ON UPDATE
-- 3. 统一 V54 表的时间戳列名 created_at/updated_at -> created_time/updated_time
-- ============================================================

-- 1. 修复 rx_ibmi_system.name 保留字
-- 先检查列是否存在再重命名（幂等）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_ibmi_system'
    AND COLUMN_NAME = 'name'
);
SET @sql = IF(@col_exists > 0,
  'ALTER TABLE `rx_ibmi_system` CHANGE COLUMN `name` `system_name` VARCHAR(100) NOT NULL COMMENT "系统名称"',
  'SELECT "rx_ibmi_system.system_name already exists" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 为 rx_user 表补全时间戳默认值（如果尚未有）
SET @has_default_created = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_user'
    AND COLUMN_NAME = 'created_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@has_default_created = 0,
  'ALTER TABLE `rx_user` MODIFY COLUMN `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_user.created_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_default_updated = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_user'
    AND COLUMN_NAME = 'updated_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@has_default_updated = 0,
  'ALTER TABLE `rx_user` MODIFY COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
  'SELECT "rx_user.updated_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 为 rx_ibmi_system 表补全时间戳默认值
SET @col_created = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_ibmi_system'
    AND COLUMN_NAME = 'created_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_created = 0,
  'ALTER TABLE `rx_ibmi_system` MODIFY COLUMN `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_ibmi_system.created_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_updated = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_ibmi_system'
    AND COLUMN_NAME = 'updated_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_updated = 0,
  'ALTER TABLE `rx_ibmi_system` MODIFY COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
  'SELECT "rx_ibmi_system.updated_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 为 rx_menu 表补全时间戳默认值 (V8 无默认值)
SET @col_created = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_menu'
    AND COLUMN_NAME = 'created_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_created = 0,
  'ALTER TABLE `rx_menu` MODIFY COLUMN `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_menu.created_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_updated = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_menu'
    AND COLUMN_NAME = 'updated_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_updated = 0,
  'ALTER TABLE `rx_menu` MODIFY COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
  'SELECT "rx_menu.updated_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 为 rx_job_schedule 表补全时间戳默认值 (V3 无默认值)
SET @col_created = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_job_schedule'
    AND COLUMN_NAME = 'created_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_created = 0,
  'ALTER TABLE `rx_job_schedule` MODIFY COLUMN `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_job_schedule.created_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_updated = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_job_schedule'
    AND COLUMN_NAME = 'updated_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_updated = 0,
  'ALTER TABLE `rx_job_schedule` MODIFY COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
  'SELECT "rx_job_schedule.updated_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 统一 V54 rx_op_template 表的时间戳列名 (created_at/updated_at -> created_time/updated_time)
-- 检查 created_at 是否存在
SET @has_created_at = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_op_template'
    AND COLUMN_NAME = 'created_at'
);
SET @sql = IF(@has_created_at > 0,
  'ALTER TABLE `rx_op_template` CHANGE COLUMN `created_at` `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_op_template.created_time already exists" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_updated_at = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_op_template'
    AND COLUMN_NAME = 'updated_at'
);
SET @sql = IF(@has_updated_at > 0,
  'ALTER TABLE `rx_op_template` CHANGE COLUMN `updated_at` `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
  'SELECT "rx_op_template.updated_time already exists" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. 为 rx_job_schedule_history 表补全 created_time 默认值
SET @col_created = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rx_job_schedule_history'
    AND COLUMN_NAME = 'created_time'
    AND COLUMN_DEFAULT IS NOT NULL
);
SET @sql = IF(@col_created = 0,
  'ALTER TABLE `rx_job_schedule_history` MODIFY COLUMN `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"',
  'SELECT "rx_job_schedule_history.created_time already has default" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;