# 数据表与权限码总览

## 10.1 业务数据表（MySQL / DB2 for i）

| 表名 | 所属模块 | 说明 |
|------|----------|------|
| `sys_user` | 系统管理 | 平台用户表 |
| `sys_role` | 系统管理 | 角色表 |
| `sys_role_menu` | 系统管理 | 角色菜单关联 |
| `sys_role_permission` | 系统管理 | 角色权限码关联 |
| `sys_user_role` | 系统管理 | 用户角色关联 |
| `rx_user_menu` | 系统管理 | 用户直接菜单授权 |
| `rx_permission` | 系统管理 | 权限码定义 |
| `rx_menu` | 系统管理 | 菜单树定义 |
| `rx_tab` | 系统管理 | 标签页定义 |
| `rx_ip_rule` | 登录安全 | IP 黑白名单 |
| `rx_login_attempt` | 登录安全 | 登录失败记录 |
| `rx_token_blacklist` | 登录安全 | JWT 吊销记录 |
| `rx_audit_log` | 审计 | 操作审计日志 |
| `rx_ibmi_system` | AS400 | IBM i 服务器配置 |
| `rx_metric` | 监控 | 指标采集数据 |
| `rx_alert_rule` | 监控 | 告警规则 |
| `rx_alert_event` | 监控 | 告警事件 |
| `rx_inspection_record` | 巡检 | 巡检记录 |
| `rx_sql_history` | SQL查询 | 查询历史 |
| `rx_command_script` | 工具集 | 命令脚本 |
| `rx_command_script_execution` | 工具集 | 脚本执行记录 |
| `rx_job_schedule` | 工具集 | 调度任务 |
| `rx_job_schedule_history` | 工具集 | 调度执行历史 |
| `rx_compile_record` | 编译 | 编译记录 |
| `rx_job_sla` | 作业中心 | SLA 规则 |
| `rx_job_dependency` | 作业中心 | 作业依赖关系 |
| `rx_topology_config` | 拓扑 | 拓扑配置 |
| `rx_config` | 系统管理 | 系统参数 |
| `rx_i18n` | 系统管理 | 国际化翻译 |
| `rx_dict_type` | 系统管理 | 字典类型 |
| `rx_dict_item` | 系统管理 | 字典项 |
| `rx_webhook` | 系统管理 | Webhook 配置 |
| `rx_webhook_log` | 系统管理 | Webhook 发送日志 |
| `rx_notification` | 系统管理 | 站内通知 |
| `rx_notice` | 系统管理 | 公告 |
| `rx_doc` | 系统管理 | 文档 |
| `rx_report` | 系统管理 | 报表 |
| `rx_favorite` | 系统管理 | 收藏夹 |
| `rx_calendar` | 系统管理 | 日历 |
| `rx_dashboard_widget` | 系统管理 | 仪表盘小组件 |
| `rx_dist_lock` | 监控 | 分布式锁 |

---

## 10.2 IBM i 系统表（QSYS2）

| 系统表 | 用途 | 对应功能 |
|--------|------|----------|
| `QSYS2.SYSTEM_STATUS_INFO` | CPU/内存/磁盘使用率 | Dashboard / 监控采集 |
| `QSYS2.ACTIVE_JOB_INFO` | 活动作业列表 | 作业中心 / 监控采集 |
| `QSYS2.JOB_QUEUE_INFO` | 作业队列信息 | 作业中心 |
| `QSYS2.OUTPUT_QUEUE_ENTRIES_BASIC` | SPOOL 文件列表 | 作业中心 |
| `QSYS2.SUBSYSTEM_INFO` | 子系统状态 | 子系统管理 |
| `QSYS2.SYSTABLES` | 库内文件清单 | 业务数据 / PF / 源码 |
| `QSYS2.SYSCOLUMNS` | 字段定义 | 业务数据 / PF / 表字段 |
| `QSYS2.SYSPARTITIONSTAT` | 源码成员 | 源码浏览 |
| `QSYS2.SYSTEM_VALUE_INFO` | 系统值 | 系统值查询 |
| `QSYS2.SYSLIMITS` | 系统限制 | 巡检 |
| `QSYS2.USER_INFO` | 用户画像 | AS400 登录认证 |

---

## 10.3 权限码汇总

| 权限码 | 所属模块 | 说明 |
|--------|----------|------|
| `USER_MANAGE` | 系统管理 | 用户管理 |
| `ROLE_MANAGE` | 系统管理 | 角色管理 |
| `MENU_MANAGE` | 系统管理 | 菜单管理 |
| `PERMISSION_MANAGE` | 系统管理 | 权限码管理 |
| `SYS_CONFIG_MANAGE` | 系统管理 | 系统配置管理 |
| `I18N_MANAGE` | 系统管理 | 国际化管理 |
| `DICT_MANAGE` | 系统管理 | 字典管理 |
| `WEBHOOK_MANAGE` | 系统管理 | Webhook 管理 |
| `NOTICE_MANAGE` | 系统管理 | 公告管理 |
| `NOTICE_VIEW` | 系统管理 | 公告查看 |
| `AUDIT_VIEW` | 审计 | 审计日志查看 |
| `SYS_IP_MANAGE` | 登录安全 | IP 规则管理 |
| `MONITOR_VIEW` | 监控 | 监控查看 |
| `ALERT_MANAGE` | 监控 | 告警规则管理 |
| `INSPECTION_VIEW` | 巡检 | 巡检查看 |
| `INSPECTION_EXECUTE` | 巡检 | 巡检执行 |
| `JOB_VIEW` | 作业中心 | 作业查看 |
| `JOB_END` | 作业中心 | 作业控制 |
| `JOB_MANAGE` | 作业中心 | SLA 管理 |
| `QUERY_EXECUTE` | SQL查询 | 查询执行 |
| `IFS_VIEW` | IFS | IFS 查看 |
| `IFS_MANAGE` | IFS | IFS 管理 |
| `DOC_MANAGE` | 文档 | 文档管理 |
| `DOC_VIEW` | 文档 | 文档查看 |
| `OBJECT_VIEW` | 对象 | 对象查看 |
| `PF_VIEW` | PF | PF 查看 |
| `SUBSYSTEM_VIEW` | 子系统 | 子系统查看 |
| `SUBSYSTEM_MANAGE` | 子系统 | 子系统管理 |
| `SCRIPT_VIEW` | 脚本 | 脚本查看 |
| `SCRIPT_MANAGE` | 脚本 | 脚本管理 |
| `SCHEDULE_VIEW` | 调度 | 调度查看 |
| `SCHEDULE_MANAGE` | 调度 | 调度管理 |
| `EXECUTION_VIEW` | 执行 | 执行历史查看 |
| `COMPILE_EXECUTE` | 编译 | 编译执行 |
| `SOURCE_VIEW` | 源码 | 源码查看 |
| `TOPOLOGY_VIEW` | 拓扑 | 拓扑查看 |
| `MSGF_VIEW` | 消息文件 | 消息文件查看 |
| `SYSVAL_VIEW` | 系统值 | 系统值查看 |
| `REPORT_VIEW` | 报表 | 报表查看 |
| `FAVORITE_VIEW` | 收藏 | 收藏查看 |
| `CALENDAR_VIEW` | 日历 | 日历查看 |
| `DASHBOARD_VIEW` | 仪表盘 | 小组件查看 |