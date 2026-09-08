-- ============================================================
-- V95: Operation module permission codes
-- ============================================================

-- Operation 权限码
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('OPERATION_EXECUTE', 'OPERATION_EXECUTE', 'SYSTEM'),
('OPERATION_VIEW', 'OPERATION_VIEW', 'SYSTEM'),
('BREAK_GLASS_EXECUTE', 'BREAK_GLASS_EXECUTE', 'SYSTEM'),
('USER_CRITICAL_AUTHORITY', 'USER_CRITICAL_AUTHORITY', 'SYSTEM');