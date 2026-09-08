-- ============================================================
-- Phase 0: Operation-Driven Architecture 核心表
-- ============================================================

-- Operation 主表：记录每次操作的生命周期
CREATE TABLE rx_operation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type  VARCHAR(50)  NOT NULL COMMENT '操作类型编码（USER_CREATE / END_JOB / IFS_WRITE / RAW_CL 等）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED/RUNNING/SUCCESS/FAILED/RETRYING/PARTIAL_SUCCESS/CANCELLED',
    current_step    VARCHAR(50)  NULL COMMENT '当前执行到的 Step 编码',
    target_type     VARCHAR(50)  NOT NULL COMMENT '操作目标类型（USER_PROFILE / JOB / IFS_FILE / SUBSYSTEM 等）',
    target_name     VARCHAR(255) NOT NULL COMMENT '操作目标标识（用户名/作业名/文件路径 等）',
    request_data    JSON         NULL COMMENT '请求参数（JSON）',
    result_data     JSON         NULL COMMENT '执行结果（JSON）',
    error_code      VARCHAR(50)  NULL COMMENT '错误码（OPERATION_TIMEOUT / STEP_FAILED 等）',
    error_message   TEXT         NULL COMMENT '错误描述',
    retry_count     INT          NOT NULL DEFAULT 0,
    max_retry       INT          NOT NULL DEFAULT 3,
    idempotency_key VARCHAR(100) NULL COMMENT '幂等键（防重复提交）',
    risk_level      VARCHAR(20)  NOT NULL DEFAULT 'READ' COMMENT 'READ/WRITE/DESTRUCTIVE/CRITICAL/BREAK_GLASS',
    requested_by    VARCHAR(100) NOT NULL COMMENT '发起人',
    requested_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at      TIMESTAMP    NULL,
    completed_at    TIMESTAMP    NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT          NOT NULL DEFAULT 1 COMMENT 'CAS 乐观锁版本号',
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_status (status),
    INDEX idx_target (target_type, target_name),
    INDEX idx_updated (updated_at)
) COMMENT 'Operation 主表';

-- Operation Step 表：记录每个步骤的执行状态
CREATE TABLE rx_operation_step (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_id     BIGINT       NOT NULL,
    step_code        VARCHAR(50)  NOT NULL COMMENT '步骤编码（CREATE_USER / SET_GROUP / SET_AUTHORITY / VERIFY 等）',
    step_order       INT          NOT NULL COMMENT '步骤顺序号（从 1 开始）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'NOT_EXECUTED' COMMENT 'NOT_EXECUTED/RUNNING/SUCCESS/FAILED/SKIPPED',
    started_at       TIMESTAMP    NULL,
    completed_at     TIMESTAMP    NULL,
    duration_ms      BIGINT       NULL COMMENT '执行耗时（毫秒）',
    ibmi_return_code VARCHAR(10)  NULL COMMENT 'IBM i 返回码（CPF0000 等）',
    ibmi_message     TEXT         NULL COMMENT 'IBM i 消息',
    error_detail     TEXT         NULL COMMENT '错误详情',
    retry_count      INT          NOT NULL DEFAULT 0,
    INDEX idx_operation (operation_id),
    UNIQUE KEY uk_op_step (operation_id, step_code)
) COMMENT 'Operation Step 执行记录';

-- rx_audit_log 新增 operation_id 外键
ALTER TABLE rx_audit_log ADD COLUMN operation_id BIGINT NULL COMMENT '关联的 Operation ID';
ALTER TABLE rx_audit_log ADD INDEX idx_operation_id (operation_id);