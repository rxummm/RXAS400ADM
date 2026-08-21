-- W3：把 DataInitializer 中「真实种子数据」下沉到 Flyway（DataInitializer 只保留 mock/dev 演示数据）。
-- 1) 默认系统参数（sys_config）：组角色映射 / Webhook 地址与语言 —— 原仅 DataInitializer.ensureDefaultConfig()
--    播种，任何环境都要有；搬到这里后每次启动不再需要 CommandLineRunner 兜底。
-- 2) 文档模板（rx_doc_template）：运维变更单 / 运维值班记录 —— 原仅 DataInitializer.initDocTemplates() 播种。
-- 说明：
-- - permission.menuDomain 不在此迁移：由 V25 种子 + V27/V28 增量维护（单源），DataInitializer 里对应分支
--   在 V25+ 库上永不执行（死代码），直接移除即可。
-- - 幂等：逐行 WHERE NOT EXISTS（config_key 主键 / 模板 name 唯一语义），重复执行不重复插入。
-- - 不建对象（仅 INSERT 存量表 rx_config / rx_doc_template），无需进 MANIFEST。

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'as400.login.groupRoleMapping', '{"GRPADM":"ADMIN","GRPDEV":"DEVELOPER","GRPOPR":"OPERATOR"}', 'AS400 组 profile → 平台角色映射（JSON）'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'as400.login.groupRoleMapping');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'alert.webhook.url', '', '告警 Webhook 推送地址（空=不推送），如企业微信/钉钉/Teams 机器人 URL'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'alert.webhook.url');

INSERT INTO rx_config (config_key, config_value, description)
SELECT 'alert.webhook.lang', 'zh-CN', 'Webhook/邮件通知语言（zh-CN=中文，en-US=英文）'
WHERE NOT EXISTS (SELECT 1 FROM rx_config WHERE config_key = 'alert.webhook.lang');

INSERT INTO rx_doc_template (name, category, content, created_by, created_time, updated_time)
SELECT '运维变更单', '变更管理',
'# ${title}

## 一、变更内容

## 二、变更原因

## 三、影响范围

## 四、实施步骤

## 五、回退方案

## 六、验证清单

- [ ] 变更前备份
- [ ] 实施完成
- [ ] 业务验证通过
', 'system', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_doc_template WHERE name = '运维变更单');

INSERT INTO rx_doc_template (name, category, content, created_by, created_time, updated_time)
SELECT '运维值班记录', '日常运维',
'# ${title}

- 值班人：
- 日期：

## 检查项
- [ ] 作业状态无 MSGW
- [ ] 磁盘使用率正常
- [ ] 备份任务成功

## 事件记录

## 交接事项
', 'system', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_doc_template WHERE name = '运维值班记录');
