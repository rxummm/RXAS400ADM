-- ============================================================
-- V97: Desired State 存储表（配置漂移检测）
-- ============================================================

CREATE TABLE IF NOT EXISTS rx_desired_state (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(50)  NOT NULL COMMENT '目标类型（USER_PROFILE / JOB_CONFIG / IFS_FILE 等）',
    target_name     VARCHAR(255) NOT NULL COMMENT '目标标识（用户名/作业名/文件路径 等）',
    state_data      JSON         NOT NULL COMMENT '期望状态（JSON）',
    version         INT          NOT NULL DEFAULT 1 COMMENT '版本号（CAS 乐观锁）',
    created_by      VARCHAR(100) NULL COMMENT '创建人',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_target (target_type, target_name),
    INDEX idx_type (target_type)
) COMMENT 'Desired State 存储表';