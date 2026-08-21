-- M1：命令脚本执行状态结构化。
-- 背景：lastResult 是中文拼接串（"成功: xxx"/"失败: xxx"），状态分类此前依赖
-- startsWith("失败")（ReportService/ExecutionService），改文案即坏逻辑，且 en-US 混排中文。
-- 新增结构化列 last_run_status（SUCCESS/FAILED），并按 lastResult 前缀回填历史数据。
-- 幂等说明：Flyway 每版本仅执行一次，ADD COLUMN 无需 IF NOT EXISTS（MySQL 8 不支持）。
ALTER TABLE rx_command_script ADD COLUMN last_run_status VARCHAR(16) NULL COMMENT '最近执行结果状态: SUCCESS/FAILED' AFTER last_result;

UPDATE rx_command_script
SET last_run_status = CASE
    WHEN last_result LIKE '失败%' THEN 'FAILED'
    WHEN last_result IS NOT NULL AND last_result <> '' THEN 'SUCCESS'
    ELSE NULL
END;