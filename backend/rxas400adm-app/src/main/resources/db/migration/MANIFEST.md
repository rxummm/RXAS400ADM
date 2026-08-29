# MANIFEST.md — 数据库对象清单

> 本文档记录所有 Flyway 迁移创建的数据库对象（表、索引、约束），供 review 和一致性校验使用。
> `check-migrations.mjs` 的 MANIFEST 断言基于此文档。

---

## 表（44 张）

| 表名 | 创建迁移 | 说明 |
|------|---------|------|
| `rx_user` | V1 | 用户表 |
| `rx_role` | V1 | 角色表 |
| `rx_permission` | V1 | 权限表 |
| `rx_user_role` | V1 | 用户-角色关联 |
| `rx_role_permission` | V1 | 角色-权限关联 |
| `rx_audit_log` | V1 | 审计日志 |
| `rx_ibmi_system` | V1 | IBM i 服务器（V65: `name` → `system_name`） |
| `rx_as400_connection_log` | V1 | AS400 连接日志 |
| `rx_source_library` | V1 | 源码库 |
| `rx_source_file` | V1 | 源码文件 |
| `rx_source_member` | V1 | 源码成员 |
| `rx_compile_record` | V1 | 编译记录 |
| `rx_metric` | V1 | 监控指标 |
| `rx_job_history` | V1 | 作业历史 |
| `rx_alert_rule` | V1 | 告警规则 |
| `rx_alert_event` | V1 | 告警事件 |
| `rx_config` | V1 | 系统配置 |
| `rx_sql_history` | V2 | SQL 执行历史 |
| `rx_job_schedule` | V3 | 作业调度 |
| `rx_job_schedule_history` | V3 | 作业调度历史 |
| `rx_login_attempt` | V4 | 登录尝试 |
| `rx_command_script` | V5 | 命令脚本 |
| `rx_metric_baseline` | V6 | 指标基线 |
| `rx_i18n` | V6 | 国际化 |
| `rx_doc_template` | V7 | 文档模板 |
| `rx_doc` | V7 | 文档 |
| `rx_doc_version` | V7 | 文档版本 |
| `rx_menu` | V8 | 菜单 |
| `rx_region` | V10 | 区域 |
| `rx_calendar_event` | V10 | 日历事件 |
| `rx_role_menu` | V11 | 角色-菜单关联 |
| `rx_webhook` | V13 | Webhook |
| `rx_webhook_log` | V13 | Webhook 日志 |
| `rx_notification` | V13 | 通知 |
| `rx_notice` | V13 | 公告 |
| `rx_permission_request` | V13 | 权限申请 |
| `rx_ip_rule` | V13 | IP 规则 |
| `rx_dashboard_widget` | V13 | 仪表板组件 |
| `rx_favorite` | V15 | 收藏 |
| `rx_dict_type` | V16 | 字典类型 |
| `rx_dict_item` | V16 | 字典项 |
| `rx_user_menu` | V17 | 用户-菜单关联 |
| `rx_report_schedule` | V26 | 报表调度 |
| `rx_report_schedule_history` | V26 | 报表调度历史 |
| `rx_job_sla` | V28 | 作业 SLA |
| `rx_scheduler_lock` | V29/V30 | 调度锁（V29/V30 重复创建，IF NOT EXISTS 幂等） |
| `rx_token_blacklist` | V36 | Token 黑名单 |
| `rx_dist_lock` | V43 | 分布式锁 |
| `rx_op_template` | V54 | 操作模板（V65: `created_at` → `created_time`） |
| `rx_sys_doc` | V66 | 知识库文档（纯 DB） |

---

## 索引

| 索引名 | 表 | 创建迁移 | 说明 |
|--------|-----|---------|------|
| `idx_audit_user_time` | `rx_audit_log` | V1 | 用户+时间查询 |
| `idx_metric_instance_time` | `rx_metric` | V1/V42 | 实例+时间查询（V42 重建） |
| `idx_job_instance` | `rx_job_history` | V1 | 实例查询 |
| `idx_user_login_source` | `rx_user` | V2 | 登录来源 |
| `idx_report_schedule_history` | `rx_report_schedule_history` | V26 | 调度+时间 |
| `idx_doc_version_doc` | `rx_doc_version` | V35 | 文档版本 |
| `idx_sql_history_operator` | `rx_sql_history` | V35 | 操作者 |
| `idx_user_as400_server` | `rx_user` | V35 | AS400 服务器 |
| `idx_ibmi_system_enabled_default` | `rx_ibmi_system` | V35 | 启用+默认 |
| `idx_alert_rule_server` | `rx_alert_rule` | V35 | 服务器 |
| `idx_alert_event_instance` | `rx_alert_event` | V35 | 实例 |
| `idx_alert_event_rule` | `rx_alert_event` | V35 | 规则 |
| `idx_alert_event_status_time` | `rx_alert_event` | V35 | 状态+时间 |
| `idx_metric_name_time` | `rx_metric` | V41 | 名称+时间 |
| `idx_job_history_name_number` | `rx_job_history` | V51 | 作业名+编号 |
| `idx_sql_history_created_time` | `rx_sql_history` | V51 | 创建时间 |
| `idx_compile_record_created_time` | `rx_compile_record` | V51 | 创建时间 |
| `idx_audit_operate_target` | `rx_audit_log` | V53 | 操作目标 |
| `idx_audit_result` | `rx_audit_log` | V53 | 结果 |
| `idx_alert_event_upgrade` | `rx_alert_event` | V55 | 升级通知去重 |
| `idx_sys_doc_status` | `rx_sys_doc` | V66 | 状态 |
| `idx_sys_doc_category` | `rx_sys_doc` | V66 | 分类 |

---

## 唯一索引

| 索引名 | 表 | 创建迁移 |
|--------|-----|---------|
| `uk_ibmi_system_name` | `rx_ibmi_system` | V40 |

---

## 外键约束（V39）

| 约束名 | 子表 → 父表 |
|--------|------------|
| `fk_job_schedule_ibmi` | `rx_job_schedule` → `rx_ibmi_system` |
| `fk_job_schedule_history_ibmi` | `rx_job_schedule_history` → `rx_ibmi_system` |
| `fk_user_ibmi` | `rx_user` → `rx_ibmi_system` |
| `fk_alert_rule_ibmi` | `rx_alert_rule` → `rx_ibmi_system` |
| `fk_report_schedule_ibmi` | `rx_report_schedule` → `rx_ibmi_system` |
| `fk_report_schedule_history_ibmi` | `rx_report_schedule_history` → `rx_ibmi_system` |
| `fk_metric_baseline_ibmi` | `rx_metric_baseline` → `rx_ibmi_system` |

---

## 已知遗留

| 问题 | 迁移 | 说明 |
|------|------|------|
| V29/V30 重复建表 `rx_scheduler_lock` | V29, V30 | IF NOT EXISTS 幂等，历史遗留不可删 |
| V31 空迁移 | V31 | AES 加密说明占位，无 DDL |
| V54 `created_at`/`updated_at` 列名不一致 | V54 | V65 已统一为 `created_time`/`updated_time` |

---

*最后更新：2026-08-28*
*覆盖迁移：V1 ~ V66（共 66 个）*
