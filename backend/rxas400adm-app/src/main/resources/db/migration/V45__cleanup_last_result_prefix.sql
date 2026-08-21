-- N1：清洗 lastResult 中文前缀（"成功: "/"失败: "），使 lastResult 只存原始消息。
-- 背景：CommandScriptService.execute() 此前拼接中文前缀写入 lastResult，
-- 现已改为只存原始 result.message()；存量数据需同步清洗。
-- 幂等说明：Flyway 每版本仅执行一次，UPDATE 对已清洗数据无副作用（LIKE 不命中 = 不改）。
UPDATE rx_command_script SET last_result = REPLACE(last_result, '成功: ', '') WHERE last_result LIKE '成功: %';
UPDATE rx_command_script SET last_result = REPLACE(last_result, '失败: ', '') WHERE last_result LIKE '失败: %';