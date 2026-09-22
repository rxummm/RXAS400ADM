-- V117: 增强用户Profile删除历史记录，添加删除原因字段

-- 为 rx_as400_user_profile_log 表添加删除原因字段
ALTER TABLE rx_as400_user_profile_log ADD COLUMN delete_reason VARCHAR(255) NULL COMMENT '删除原因（仅DELETE操作有效）';
ALTER TABLE rx_as400_user_profile_log ADD COLUMN deletion_type VARCHAR(50) NULL COMMENT '删除类型：MANUAL/INACTIVE_90D/AUTO_EXPIRE';

-- 更新现有DELETE操作记录，标记为手动删除
UPDATE rx_as400_user_profile_log SET deletion_type = 'MANUAL' WHERE action = 'DELETE' AND deletion_type IS NULL;
