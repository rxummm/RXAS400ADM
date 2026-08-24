-- V53: 审计日志增强 - 添加操作对象、执行结果、耗时字段
-- E3: 操作审计日志增强

ALTER TABLE rx_audit_log
    ADD COLUMN operate_target VARCHAR(200) COMMENT '操作对象（如作业名、文件路径、表名等）' AFTER target,
    ADD COLUMN result VARCHAR(20) COMMENT '操作结果（SUCCESS/FAIL）' AFTER detail,
    ADD COLUMN cost_ms BIGINT COMMENT '执行耗时（毫秒）' AFTER result;

-- 添加索引便于按操作对象查询
CREATE INDEX idx_audit_operate_target ON rx_audit_log (operate_target);
CREATE INDEX idx_audit_result ON rx_audit_log (result);
