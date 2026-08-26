-- V57：运行时可调配置入库（第六章·配置前端可维护化 P1~P3）
-- 将原先散落在 application.yml 的运营期可调参数迁入 rx_config，
-- 管理员通过「系统配置」页即可调整并实时生效（消费方带 60s 缓存/降级）。
-- 幂等：全部 WHERE NOT EXISTS，可重复执行。

-- ---------- CL 高危动词黑名单（DangerousClCommandValidator） ----------
INSERT INTO rx_config (config_key, config_value, description)
SELECT 'cl.blacklist.enabled', 'true',
       'CL高危动词黑名单开关(true/false)，false时所有CL命令不做黑名单拦截'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'cl.blacklist.enabled');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'cl.blacklist.extra-verbs', '',
       'CL黑名单追加动词(逗号分隔，如 CHGJOB,ENDSBS)；内置17个基线动词随代码版本维护'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'cl.blacklist.extra-verbs');

-- ---------- 登录限流阈值（RateLimitFilter） ----------
INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.rate-limit.enabled', 'true',
       '接口限流总开关(true/false)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.rate-limit.enabled');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.rate-limit.login-per-minute', '5',
       '登录接口每分钟允许次数(每IP)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.rate-limit.login-per-minute');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.rate-limit.command-per-minute', '10',
       'CL命令执行接口每分钟允许次数(每IP)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.rate-limit.command-per-minute');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.rate-limit.api-per-minute', '60',
       '通用API每分钟允许次数(每IP)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.rate-limit.api-per-minute');

-- ---------- 登录失败锁定阈值（LoginAttemptService） ----------
INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.login.max-failed', '5',
       '同一用户+服务器连续登录失败多少次后锁定'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.login.max-failed');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.login.lock-minutes', '15',
       '登录失败锁定时长(分钟)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.login.lock-minutes');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'security.login.max-ip-per-minute', '20',
       '同一IP每分钟最大登录尝试次数'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'security.login.max-ip-per-minute');

-- ---------- 告警升级通知角色（AlertUpgradeService） ----------
INSERT INTO rx_config (config_key, config_value, description)
SELECT 'alert.upgrade.notify-role', 'ADMIN',
       '告警超时未处理升级通知的目标角色编码(如 ADMIN/OPERATOR)'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'alert.upgrade.notify-role');
