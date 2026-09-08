-- C-SYS005/006: TOCTOU race condition fixes via UNIQUE constraints
-- rx_permission_request: prevent duplicate PENDING requests for same user+permission
SET @exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'rx_permission_request' AND index_name = 'uk_permission_request_pending');
SET @sql = IF(@exists = 0, 'CREATE UNIQUE INDEX uk_permission_request_pending ON rx_permission_request (username, permission_code, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rx_user_menu: prevent duplicate user-menu assignments
SET @exists2 = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'rx_user_menu' AND index_name = 'uk_user_menu');
SET @sql2 = IF(@exists2 = 0, 'CREATE UNIQUE INDEX uk_user_menu ON rx_user_menu (user_id, menu_id)', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
