-- V98: rx_operation_step 增加 version 字段用于 CAS 乐观锁
-- 对应 OperationStep 实体中 @Version 注解

ALTER TABLE rx_operation_step
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT 'CAS 乐观锁版本号' AFTER retry_count;

-- 已有数据的版本号从 1 开始
UPDATE rx_operation_step SET version = 1 WHERE version IS NULL OR version = 0;