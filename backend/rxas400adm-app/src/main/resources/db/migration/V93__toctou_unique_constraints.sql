-- C-SYS005/006: TOCTOU race condition fixes via UNIQUE constraints
-- rx_permission_request: prevent duplicate PENDING requests for same user+permission
CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_request_pending
    ON rx_permission_request (user_id, permission_code, status);

-- rx_user_menu: prevent duplicate user-menu assignments
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_menu
    ON rx_user_menu (user_id, menu_id);
