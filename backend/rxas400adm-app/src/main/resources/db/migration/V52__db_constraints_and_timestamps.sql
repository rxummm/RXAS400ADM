-- P2: 补 NOT NULL 约束 + updated_time 审计字段 + TIMESTAMP→DATETIME 统一

-- rx_user.status 补 NOT NULL（业务必填）
ALTER TABLE rx_user MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- rx_role.role_code 补 NOT NULL（业务必填）
ALTER TABLE rx_role MODIFY COLUMN role_code VARCHAR(50) NOT NULL;

-- rx_ibmi_system 补 updated_time 审计字段
ALTER TABLE rx_ibmi_system ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- rx_metric 确保 collect_time 为 DATETIME（非 TIMESTAMP，避免 2038 问题）
-- 已有 DATETIME 类型，无需修改；加注释标记
-- rx_job_schedule_history 补 updated_time
ALTER TABLE rx_job_schedule_history ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- rx_doc 补 updated_time
ALTER TABLE rx_doc ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
