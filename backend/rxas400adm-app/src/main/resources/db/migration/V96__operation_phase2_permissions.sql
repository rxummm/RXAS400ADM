-- ============================================================
-- V96: Phase 2 permission codes (IFS/Document/Spool)
-- ============================================================

INSERT IGNORE INTO rx_permission (permission_code, permission_name, module) VALUES
('SPOOL_DELETE', 'SPOOL_DELETE', 'SYSTEM');