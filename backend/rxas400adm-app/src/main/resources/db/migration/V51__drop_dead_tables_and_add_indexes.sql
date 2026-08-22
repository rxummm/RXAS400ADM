-- D1: 删除 4 张死表（V1 创建，对应功能从未落地，无任何代码引用）
-- 已发布迁移不可改，故以新迁移反向清理（幂等：IF EXISTS）
DROP TABLE IF EXISTS rx_as400_connection_log;
DROP TABLE IF EXISTS rx_source_library;
DROP TABLE IF EXISTS rx_source_file;
DROP TABLE IF EXISTS rx_source_member;

-- D2: 补缺失索引，避免大表全表扫描（幂等：information_schema 预查 + PREPARE 动态 DDL）
SET @s = (SELECT COUNT(*) FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rx_job_history'
            AND INDEX_NAME = 'idx_job_history_name_number');
SET @sql = IF(@s = 0,
    'CREATE INDEX idx_job_history_name_number ON rx_job_history(job_name, job_number)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT COUNT(*) FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rx_sql_history'
            AND INDEX_NAME = 'idx_sql_history_created_time');
SET @sql = IF(@s = 0,
    'CREATE INDEX idx_sql_history_created_time ON rx_sql_history(created_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT COUNT(*) FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rx_compile_record'
            AND INDEX_NAME = 'idx_compile_record_created_time');
SET @sql = IF(@s = 0,
    'CREATE INDEX idx_compile_record_created_time ON rx_compile_record(created_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
