-- W1：清洗 rx_job_schedule.last_result 的状态前缀（"SUCCESS: "/"FAILED: "），
-- 与 N1 的 V45 同款——lastResult 只存原始消息，成功/失败前缀由前端按 status 列渲染。
-- 背景：JobScheduleService.execute() 此前写入 "SUCCESS: 查询成功，返回 N 行" 等带前缀+中文的
-- 混合文本，现已改为只存原始消息（SQL 成功存行数、CL 成功存命令输出）；存量数据需同步清洗。
-- 幂等说明：Flyway 每版本仅执行一次；UPDATE 对已清洗数据无副作用（LIKE 不命中 = 不改）。
UPDATE rx_job_schedule SET last_result = REPLACE(last_result, 'SUCCESS: ', '') WHERE last_result LIKE 'SUCCESS: %';
UPDATE rx_job_schedule SET last_result = REPLACE(last_result, 'FAILED: ', '') WHERE last_result LIKE 'FAILED: %';
