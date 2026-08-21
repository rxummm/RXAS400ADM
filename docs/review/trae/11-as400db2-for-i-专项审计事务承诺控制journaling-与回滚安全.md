---
title: "十一、AS400/DB2 for i 专项审计：事务承诺控制、Journaling 与回滚安全"
---

# 十一、AS400/DB2 for i 专项审计：事务承诺控制、Journaling 与回滚安全

> **审计日期**：2026-08-15（第四轮专项审计 —— AS400 部署环境）  
> **审计范围**：全量 `@Transactional` 注解（20 个 Service，51 个方法）、AS400 JDBC 连接配置、Journaling 日志要求、长事务风险  
> **核心原则**：AS400 (IBM i) 底层 DB2 for i 的物理表（PF）必须**显式开启 Journaling（STRJRNPF）**才能支持事务回滚。未开启日志的表在事务中执行写操作时，回滚将**静默失效**，导致数据不一致且无任何错误提示。

---

### 11.1 `@Transactional(rollbackFor = Exception.class)` 全景扫描

#### 🛑 [严重/Critical] 14 个 Service 共 33 个方法缺少 `rollbackFor = Exception.class`

> **AAA-Remark（2026-08-15 复核）**：✅ **真实存在**——14 个 Service 缺 `rollbackFor`（Doc/Webhook/Notification/SysUserServiceImpl/PermissionRequest/Notice/Region/CalendarEvent/IpRule/ReportSchedule/JobSchedule/Baseline/DashboardWidget/As400Login），正面教材 6 个 Service 名单也验证一致（Dict/Favorite/Menu/PermissionManage/Role/UserMenu）。⚠️ 数字修正：实测裸 `@Transactional`（无 rollbackFor）**55 处**、带 rollbackFor 22 处——「33 个方法」按方法级口径偏低（55 处含部分非方法级注解与同方法多注解），以「55 处注解」为准更稳。另注意：`BusinessException` 继承自 `RuntimeException`，当前业务代码抛的 checked exception 很少，**实际触发场景有限**；但统一 `rollbackFor = Exception.class` 仍是低风险高收益的规范动作（§8.1 与 §11.1 口径差异：8.1 抽查 7 个、11.1 全量 14 个，以 11.1 为准）。

**问题定性**：属于 **"AS400 事务控制与物理表日志适配"** 问题。Spring 的 `@Transactional` 默认仅在 `RuntimeException` 和 `Error` 时回滚。`BusinessException` 虽然继承自 `RuntimeException`，但项目中的 `IOException`、`SQLException` 等 checked exception 会导致事务**不回滚**。在 AS400 环境下，不回滚的事务会留下**半提交的脏数据**——因为 Journaling 日志未被用于回滚，数据直接写入物理表 PF，无法逆转。

**全景统计表**：

| #   | Service 文件               | 方法数 | 缺失 `rollbackFor` | 涉及表                                                   | 风险等级 |
| --- | -------------------------- | ------ | ------------------ | -------------------------------------------------------- | -------- |
| 1   | `DocService`               | 8      | **全部 8 个**      | `rx_doc`, `rx_doc_version`, `rx_doc_template`            | 🔴 严重  |
| 2   | `WebhookService`           | 5      | **全部 5 个**      | `rx_webhook`, `rx_webhook_log`                           | 🔴 严重  |
| 3   | `NotificationService`      | 6      | **全部 6 个**      | `rx_notification`                                        | 🔴 严重  |
| 4   | `SysUserServiceImpl`       | 4      | **全部 4 个**      | `rx_user`, `rx_user_role`                                | 🔴 严重  |
| 5   | `PermissionRequestService` | 3      | **全部 3 个**      | `rx_permission_request`, `rx_user_menu`, `rx_permission` | 🔴 严重  |
| 6   | `NoticeService`            | 3      | **全部 3 个**      | `rx_notice`                                              | 🟡 警告  |
| 7   | `RegionService`            | 3      | **全部 3 个**      | `rx_region`                                              | 🟡 警告  |
| 8   | `CalendarEventService`     | 3      | **全部 3 个**      | `rx_calendar_event`                                      | 🟡 警告  |
| 9   | `IpRuleService`            | 3      | **全部 3 个**      | `rx_ip_rule`                                             | 🟡 警告  |
| 10  | `ReportScheduleService`    | 5      | **全部 5 个**      | `rx_report_schedule`, `rx_report_schedule_history`       | 🔴 严重  |
| 11  | `JobScheduleService`       | 5      | **全部 5 个**      | `rx_job_schedule`, `rx_job_schedule_history`             | 🔴 严重  |
| 12  | `BaselineService`          | 1      | **全部 1 个**      | `rx_metric_baseline`                                     | 🟡 警告  |
| 13  | `DashboardWidgetService`   | 1      | **全部 1 个**      | `rx_dashboard_widget`                                    | 🟡 警告  |
| 14  | `As400LoginService`        | 1      | **全部 1 个**      | `rx_user`, `rx_token_blacklist`                          | 🔴 严重  |

**已正确使用 `rollbackFor = Exception.class` 的 6 个 Service（正面教材）**：

| Service                   | 方法数 | 文件                                                                                                                                                                 |
| ------------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DictService`             | 6      | [DictService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/DictService.java)                         |
| `FavoriteService`         | 2      | [FavoriteService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/FavoriteService.java)                 |
| `PermissionManageService` | 3      | [PermissionManageService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/PermissionManageService.java) |
| `MenuService`             | 4      | [MenuService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/MenuService.java)                         |
| `RoleService`             | 4      | [RoleService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/RoleService.java)                         |
| `UserMenuService`         | 3      | [UserMenuService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/UserMenuService.java)                 |

**🔍 原因分析（AS400 特有风险）**：

1. **SQL7008 错误风险**：当一个 `@Transactional` 方法操作了**未开启 Journaling** 的表时，AS400 的 DB2 for i 在事务提交时会尝试写日志，但发现日志不存在，抛出 `SQL7008: <table> in <library> not valid for operation`。这意味着即使代码逻辑正确，只要运维未执行 `STRJRNPF`，事务就会在运行时崩溃。

2. **回滚静默失效风险**：更危险的情况是，如果 Spring 事务管理器配置了 `auto-commit` 模式或数据库默认允许无日志写入，`@Transactional` 的回滚操作将**完全无效**——数据直接写入物理表，AS400 不会报错，但数据一致性已被破坏。

3. **`rollbackFor` 缺失的叠加效应**：当 `DocService.createDoc()` 在插入 `rx_doc` 后调用 `snapshot()` 插入 `rx_doc_version` 时，如果第二步失败，第一步的 `rx_doc` 插入不会被回滚（因为 `rollbackFor` 缺失 + 可能无 Journaling），导致"孤儿文档"——文档表有记录但版本表为空。

**🛠️ 重构方案**：

**步骤一：统一修复所有 `@Transactional` 注解**

```java
// ❌ 原代码（DocService.java L90-L101）
@Transactional
public Doc createDoc(Doc doc, String operator) {
    doc.setId(null);
    doc.setVersion(1);
    doc.setStatus(STATUS_DRAFT);
    // ...
    docMapper.insert(doc);
    snapshot(doc, operator);
    return doc;
}

// ✅ 重构后：显式声明 rollbackFor
@Transactional(rollbackFor = Exception.class)
public Doc createDoc(Doc doc, String operator) {
    doc.setId(null);
    doc.setVersion(1);
    doc.setStatus(STATUS_DRAFT);
    // ...
    docMapper.insert(doc);
    snapshot(doc, operator);
    return doc;
}
```

**步骤二：批量修复脚本（IDE 正则替换）**

在 IntelliJ IDEA 中使用正则替换：
- **查找**：`@Transactional\n`（不带参数的 `@Transactional`）
- **替换为**：`@Transactional(rollbackFor = Exception.class)\n`
- **排除**：已有 `rollbackFor` 的 6 个 Service

**步骤三：添加 Checkstyle / SonarQube 规则**

```xml
<module name="Regexp">
  <property name="format" value="@Transactional\s*$"/>
  <property name="message" value="@Transactional 必须显式声明 rollbackFor = Exception.class"/>
</module>
```

### 11.2 AS400 Journaling 日志要求：全量表映射与运维清单

核心概念：AS400/DB2 for i 中，物理表（PF）默认不开启 Journaling。只有执行 STRJRNPF 命令后，该表的 INSERT/UPDATE/DELETE 才会被记录到 Journal Receiver，事务回滚机制才能生效。未开启 Journaling 的表 = 事务回滚无效。

🛑 [严重/Critical] 全量 28 张核心表需在 AS400 端强制开启 Journaling
问题定性：属于 "AS400 事务控制与物理表日志适配" 问题。下表列出了项目中所有 @TableName 物理表对应的 AS400 表名，以及它们的事务操作类型和 Journaling 必要性。

全量表映射与 Journaling 要求：

| # | Entity 类 | AS400 物理表名 | 事务操作 | Journaling 必要性 | 运维命令 |
| --- | --- | --- | --- | --- | --- |
| 1 | SysUser | rx_user | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_USER) JRN(LIB/QSQJRN) |
| 2 | SysRole | rx_role | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_ROLE) JRN(LIB/QSQJRN) |
| 3 | SysMenu | rx_menu | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_MENU) JRN(LIB/QSQJRN) |
| 4 | SysPermission | rx_permission | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_PERMISSION) JRN(LIB/QSQJRN) |
| 5 | SysUserRole | rx_user_role | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_USER_ROLE) JRN(LIB/QSQJRN) |
| 6 | SysUserMenu | rx_user_menu | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_USER_MENU) JRN(LIB/QSQJRN) |
| 7 | SysRoleMenu | rx_role_menu | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_ROLE_MENU) JRN(LIB/QSQJRN) |
| 8 | SysRolePermission | rx_role_permission | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_ROLE_PERMISSION) JRN(LIB/QSQJRN) |
| 9 | SysConfig | rx_config | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_CONFIG) JRN(LIB/QSQJRN) |
| 10 | Doc | rx_doc | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DOC) JRN(LIB/QSQJRN) |
| 11 | DocVersion | rx_doc_version | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DOC_VERSION) JRN(LIB/QSQJRN) |
| 12 | DocTemplate | rx_doc_template | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DOC_TEMPLATE) JRN(LIB/QSQJRN) |
| 13 | WebhookConfig | rx_webhook | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_WEBHOOK) JRN(LIB/QSQJRN) |
| 14 | WebhookLog | rx_webhook_log | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_WEBHOOK_LOG) JRN(LIB/QSQJRN) |
| 15 | Notification | rx_notification | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_NOTIFICATION) JRN(LIB/QSQJRN) |
| 16 | Notice | rx_notice | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_NOTICE) JRN(LIB/QSQJRN) |
| 17 | Region | rx_region | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_REGION) JRN(LIB/QSQJRN) |
| 18 | CalendarEvent | rx_calendar_event | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_CALENDAR_EVENT) JRN(LIB/QSQJRN) |
| 19 | IpRule | rx_ip_rule | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_IP_RULE) JRN(LIB/QSQJRN) |
| 20 | DictType | rx_dict_type | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DICT_TYPE) JRN(LIB/QSQJRN) |
| 21 | DictItem | rx_dict_item | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DICT_ITEM) JRN(LIB/QSQJRN) |
| 22 | Favorite | rx_favorite | INSERT/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_FAVORITE) JRN(LIB/QSQJRN) |
| 23 | DashboardWidget | rx_dashboard_widget | INSERT/UPDATE | 🔴 强制 | STRJRNPF FILE(LIB/RX_DASHBOARD_WIDGET) JRN(LIB/QSQJRN) |
| 24 | PermissionRequest | rx_permission_request | INSERT/UPDATE | 🔴 强制 | STRJRNPF FILE(LIB/RX_PERMISSION_REQUEST) JRN(LIB/QSQJRN) |
| 25 | AuditLog | rx_audit_log | INSERT | 🔴 强制 | STRJRNPF FILE(LIB/RX_AUDIT_LOG) JRN(LIB/QSQJRN) |
| 26 | TokenBlacklist | rx_token_blacklist | INSERT | 🔴 强制 | STRJRNPF FILE(LIB/RX_TOKEN_BLACKLIST) JRN(LIB/QSQJRN) |
| 27 | LoginAttempt | rx_login_attempt | INSERT/UPDATE | 🔴 强制 | STRJRNPF FILE(LIB/RX_LOGIN_ATTEMPT) JRN(LIB/QSQJRN) |
| 28 | I18nEntry | rx_i18n | INSERT/UPDATE/DELETE | 🔴 强制 | STRJRNPF FILE(LIB/RX_I18N) JRN(LIB/QSQJRN) |
注意：项目中的定时任务表（rx_report_schedule, rx_report_schedule_history, rx_job_schedule, rx_job_schedule_history）在 rxas400adm-app 和 rxas400adm-as400 模块中，未出现在 @TableName 扫描结果中，但这些表同样需要 Journaling。

> **AAA-Remark（2026-08-15 复核）**：✅ 概念正确——DB2 for i 物理表默认不开启 Journaling，`STRJRNPF IMAGES(*BOTH)` 是事务回滚的运维前置条件，SQL7008 风险真实存在。⚠️ **重要背景**：本仓库当前主数据源是 **MySQL 8**（库名 rxas400adm，Flyway 管理，无 Journaling 概念），AS400 访问走 `AS400Client` 抽象（mock 开发 / JT400 生产），`JTOpenAS400Client` 为只读查询连接（QSYS2 系统表）。因此本节的 Journaling 全量表属于**面向未来 AS400 数据源迁移的运维清单**，非当前缺陷——但 `rollbackFor` 统一（§11.1）不依赖此背景，可直接做。表名映射已按 @TableName 核对，28 张表名准确（定时任务 4 表在 app/as400 模块，确实不在 system 的 @TableName 扫描内）。
>
> **结论（2026-08-15 追加）**：**AS400 主数据源未立项前，无需为应用开任何 Journaling**——当前业务表全在 MySQL（InnoDB 天然回滚），`JTOpenAS400Client` 只读 QSYS2 + 命令执行不涉及事务。若未来立项把写数据源迁到 DB2，届时**全量 32 表**（本表 28 张 + rx_report_schedule / rx_report_schedule_history / rx_job_schedule / rx_job_schedule_history 4 张定时任务表）一次性 `STRJRNPF IMAGES(*BOTH)` 开齐，不要只开「当前有写操作的表」（表会组合进同一事务，漏一张整个事务回滚静默失效）；测试环境优先「也开 Journaling」而非 §12 的关闭事务开关。**同一批次（2026-08-15）§11.1 的 55 处裸 `@Transactional` 已全量补齐 `rollbackFor = Exception.class` 并建门禁 `scripts/check-transactional.sh`**——该动作与 Journaling 无关，MySQL 下已立即生效。

🔍 原因分析：

AS400 物理表（PF）的默认行为：在 AS400 上创建的物理表默认 不开启 Journaling。这意味着 INSERT/UPDATE/DELETE 操作直接写入磁盘，无法被 ROLLBACK 撤销。

SQL7008 错误场景：当 Spring 的 DataSourceTransactionManager 尝试对一个未开启 Journaling 的表执行 COMMIT 或 ROLLBACK 时，DB2 for i 会返回 SQL7008 错误码。这在生产环境中表现为：

用户创建文档 → 前端显示成功 → 实际数据库已写入 → 但事务日志报 SQL7008
如果后续步骤失败需要回滚 → 回滚同样报 SQL7008 → 数据不一致
JT400 JDBC 驱动的默认行为：JT400（JTOpen）驱动默认 autoCommit=true，且 transactionIsolation 未设置时使用 DB2 默认的 READ COMMITTED（CS - Cursor Stability）。在这种配置下，如果物理表未开启 Journaling，驱动的 commit() 和 rollback() 调用都会触发 SQL7008。

🛠️ 重构方案（运维 + 代码双管齐下）：

方案 A：AS400 运维端初始化脚本（必须执行）

```text
-- AS400 CL 脚本：为所有业务表开启 Journaling
-- 前提：已创建 Journal Receiver（CRTJRNRCV）和 Journal（CRTJRN）
-- 以下以 LIBRARY = RXAS400ADM 为例

-- 1. 创建 Journal Receiver（日志接收器） CRTJRNRCV JRNRCV(RXAS400ADM/RXJRN0001) THRESHOLD(500000) AUTODLT(*YES)

-- 2. 创建 Journal（日志） CRTJRN JRN(RXAS400ADM/QSQJRN) JRNRCV(RXAS400ADM/RXJRN0001) MNGRCV(*SYSTEM) DLTRCV(*YES)

-- 3. 为核心业务表开启 Journaling（按影响范围从大到小） STRJRNPF FILE(RXAS400ADM/RX_USER) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_ROLE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_MENU) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_PERMISSION) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_USER_ROLE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_USER_MENU) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_ROLE_MENU) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_ROLE_PERMISSION) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_CONFIG) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DOC) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DOC_VERSION) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DOC_TEMPLATE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_WEBHOOK) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_WEBHOOK_LOG) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_NOTIFICATION) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_NOTICE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_REGION) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_CALENDAR_EVENT) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_IP_RULE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DICT_TYPE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DICT_ITEM) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_FAVORITE) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_DASHBOARD_WIDGET) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_PERMISSION_REQUEST) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_AUDIT_LOG) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_TOKEN_BLACKLIST) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_LOGIN_ATTEMPT) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH) STRJRNPF FILE(RXAS400ADM/RX_I18N) JRN(RXAS400ADM/QSQJRN) IMAGES(*BOTH)

-- 4. 验证：查询所有已开启 Journaling 的表
SELECT TABLE_NAME, JOURNAL_NAME, JOURNAL_STATUS
FROM QSYS2.SYSTABLES
WHERE TABLE_SCHEMA = 'RXAS400ADM' AND JOURNAL_NAME IS NOT NULL;
```

> **关键参数说明**：`IMAGES(*BOTH)` 表示同时记录 Before Image 和 After Image，这是事务回滚的前提条件。如果只记录 `*AFTER`，只能用于审计，不能用于回滚。

**方案 B：AS400 JDBC 连接串配置（必须确认）**

```properties
# application-as400.yml（Spring 主数据源连接 AS400/DB2 for i）
spring.datasource.url=jdbc:as400://<host>/<library>;transaction isolation=read committed;naming=system;libraries=RXAS400ADM
spring.datasource.driver-class-name=com.ibm.as400.access.AS400JDBCDriver

# 关键：显式关闭 auto-commit，由 Spring 事务管理器接管
spring.datasource.hikari.auto-commit=false

# 事务隔离级别：READ_COMMITTED（对应 DB2 Cursor Stability）
spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED
```

**方案 C：`JTOpenAS400Client` 连接配置修复（AS400 直连查询）**

```java
// JTOpenAS400Client.java
// 当前代码：缺少 transactionIsolation 和 autoCommit 设置

// 重构后：显式配置事务隔离级别
private AS400JDBCDataSource dataSource() {
    AS400JDBCDataSource ds = dataSource;
    if (ds == null) {
        synchronized (dsLock) {
            ds = dataSource;
            if (ds == null) {
                ds = new AS400JDBCDataSource();
                ds.setServerName(host);
                ds.setUser(user);
                ds.setPassword(password);
                ds.setNaming("system");
                ds.setLibraries("QSYS2");
                // P4-1：纯查询连接无需事务，显式关闭 auto-commit 避免 SQL7008
                ds.setTransactionIsolation("none"); // 纯查询不走事务
                ds.setAutoCommit(true); // 每条查询独立提交
                try {
                    ds.setLoginTimeout(15);
                } catch (SQLException e) {
                    log.warn("设置 JT400 登录超时失败(host={}): {}", host, redact(e.getMessage()));
                }
                dataSource = ds;
            }
        }
    }
    return ds;
}
```

> **设计说明**：`JTOpenAS400Client` 仅用于查询 AS400 系统表（`QSYS2.OBJECT_STATISTICS` 等），不涉及业务表的写操作，因此设置 `transactionIsolation=none` + `autoCommit=true` 是最安全的配置——既避免了 `SQL7008` 错误（因为纯查询不需要 Journaling），也确保了每条查询独立执行不互相阻塞。

---

### 11.3 长事务风险审计：事务内包含外部 I/O 调用

#### 🛑 [严重/Critical] 8 个事务方法内包含 STOMP 推送、HTTP 请求、PDF 生成、远程 AS400 查询

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——`ReportScheduleService.execute()`（L110 `@Transactional` 无 rollbackFor，内含报告生成/邮件）、`JobScheduleService.execute()`（L116 同，内含 `client.execute()` 远程命令）、`As400LoginService.login()`（L68 `@Transactional` 内含 `client.authenticate()`）均已核对。⚠️ 严重度在**当前 MySQL 数据源下应降级**：SQL0913/CPF5027 是 AS400 锁超时场景，MySQL 下锁行为不同；但「事务内做耗时 I/O」本身仍是通用坏味道，拆事务建议成立（P1 级）。事件异步化（方案 C）需先引入 `@EnableAsync`（当前无），改动面小。

**问题定性**：属于 **"AS400 事务控制"** 中的长事务风险。在 AS400 环境下，事务期间持有行锁（Row Lock），如果事务内包含耗时的外部 I/O 操作（STOMP 广播、邮件发送、PDF 生成、远程 AS400 命令执行），会导致锁持有时间过长，引发 `SQL0913`（死锁超时）或 `CPF5027`（锁等待超时）。

**全景扫描（按风险从高到低排列）**：

| # | Service | 方法 | 事务内 I/O 操作 | 风险 | AS400 错误 |
|---|---------|------|----------------|------|-----------|
| 1 | `ReportScheduleService` | `execute()` | `reportService.generate()`（PDF/Excel 生成）+ `emailService.send()`（SMTP 邮件） | 🔴 极高 | SQL0913 锁超时 |
| 2 | `JobScheduleService` | `execute()` | `client.queryList()`（远程 AS400 SQL 查询） | 🔴 极高 | SQL0913 锁超时 |
| 3 | `As400LoginService` | `login()` | `client.authenticate()`（远程 AS400 认证） | 🔴 高 | CPF5027 连接超时 |
| 4 | `NoticeService` | `create()` / `update()` | `notificationService.sendToAllActiveUsers()`（STOMP 广播） | 🟡 中 | 行锁时间延长 |
| 5 | `NotificationService` | `send()` / `sendToAllActiveUsers()` | `broadcast()`（STOMP 逐用户推送） | 🟡 中 | 行锁时间延长 |
| 6 | `SysUserServiceImpl` | `create()` / `update()` / `delete()` | `eventPublisher.publishEvent()`（同步事件） | 🟡 中 | 行锁时间延长 |
| 7 | `PermissionRequestService` | `approve()` | `eventPublisher.publishEvent()` + `notificationService.send()` | 🟡 中 | 行锁时间延长 |
| 8 | `PermissionRequestService` | `reject()` | `notificationService.send()`（STOMP 推送） | 🟡 中 | 行锁时间延长 |

**🔍 原因分析（AS400 特有风险）**：

1. **SQL0913 死锁超时**：AS400 的 DB2 for i 默认锁等待超时为 60 秒（`QSQWAITTIME` 系统值）。当 `ReportScheduleService.execute()` 在事务中先生成 PDF（可能耗时 10-30 秒），再发送邮件（可能耗时 5-15 秒），在此期间 `rx_report_schedule_history` 表的 INSERT 持有的行锁一直未释放。如果另一个定时任务恰好同时触发，第二个事务会等待第一个事务释放锁，超过 60 秒后抛出 `SQL0913`。

2. **CPF5027 锁等待超时**：AS400 的 CL 命令（如 `DSPOBJD`）在操作对象时也需要对象锁。虽然 `JTOpenAS400Client` 的连接与 Spring 主数据源隔离，但 `As400LoginService.login()` 方法中的 `client.authenticate()` 在事务内执行，如果 AS400 认证因网络延迟耗时 5-10 秒，`rx_user` 表的行锁将被持有整个认证期间。

3. **同步事件监听器在事务内执行**：`SysUserServiceImpl` 和 `PermissionRequestService` 中使用 `eventPublisher.publishEvent()` 发布事件，Spring 默认同步执行事件监听器。如果 `PermissionCacheEventListener` 中有数据库操作（如刷新 Caffeine 缓存时查询数据库），这些操作都在同一个事务内执行，增加了事务时长。

**🛠️ 重构方案**：

**方案 A：`ReportScheduleService.execute()` 和 `JobScheduleService.execute()` —— 事务拆分**

```java
// ❌ 原代码：PDF 生成 + 邮件发送 + 历史写入全在一个事务中
// ReportScheduleService.java
@Transactional
public Map<String, Object> execute(Long id) {
    ReportSchedule schedule = require(id);
    // ⚠️ 1. PDF 生成（耗时 10-30s）—— 在事务内！
    byte[] data = reportService.generate(schedule.getReportType(), schedule.getFormat(), schedule.getServerId(), days);
    // ⚠️ 2. 邮件发送（耗时 5-15s）—— 在事务内！
    emailService.send(schedule.getEmail(), subject, body, attachment);
    // 3. 历史记录写入
    historyMapper.insert(history);
    return Map.of("status", status, "message", message);
}

// ✅ 重构后：拆分为两个独立事务 + 补偿逻辑
public Map<String, Object> execute(Long id) {
    // 第一步：在事务外完成耗时 I/O 操作
    ReportSchedule schedule = scheduleMapper.selectById(id);
    byte[] data = reportService.generate(schedule.getReportType(), ...);

    // 第二步：短事务仅负责写入历史记录
    return saveHistory(id, data, status, message);
}

@Transactional(rollbackFor = Exception.class)
private Map<String, Object> saveHistory(Long id, byte[] data, String status, String message) {
    ReportSchedule schedule = scheduleMapper.selectById(id);
    schedule.setLastRunTime(LocalDateTime.now());
    schedule.setUpdatedTime(LocalDateTime.now());
    scheduleMapper.updateById(schedule);

    ReportScheduleHistory history = new ReportScheduleHistory();
    history.setScheduleId(id);
    history.setStatus(status);
    history.setMessage(message);
    history.setFileBytes(data.length);
    history.setCreatedTime(LocalDateTime.now());
    historyMapper.insert(history);

    return Map.of("status", status, "message", message);
}
```

**方案 B：`As400LoginService.login()` —— 认证与持久化分离**

```java
// ❌ 原代码：远程 AS400 认证 + 用户创建/更新 + Token 生成全在一个事务中
// As400LoginService.java
@Transactional
public LoginResponse login(As400LoginRequest request) {
    AS400Client client = clientProvider.forServer(request.getServerId());
    // ⚠️ 远程 AS400 认证（耗时 2-10s）—— 在事务内！
    if (!client.authenticate(request.getUsername(), request.getPassword())) {
        throw new BusinessException(ErrorCode.LOGIN_FAILED, "AS400 账号或密码错误");
    }
    // 用户创建/更新 + Token 生成
    SysUser user = resolveOrCreateUser(client, request);
    // ...
}

// ✅ 重构后：认证在事务外，仅持久化在事务内
public LoginResponse login(As400LoginRequest request) {
    // 第一步：远程认证（事务外，避免锁持有）
    AS400Client client = clientProvider.forServer(request.getServerId());
    if (!client.authenticate(request.getUsername(), request.getPassword())) {
        throw new BusinessException(ErrorCode.LOGIN_FAILED, "AS400 账号或密码错误");
    }
    // 第二步：用户持久化 + Token（短事务）
    return doLogin(client, request);
}

@Transactional(rollbackFor = Exception.class)
private LoginResponse doLogin(AS400Client client, As400LoginRequest request) {
    SysUser user = resolveOrCreateUser(client, request);
    List<String> permissions = permissionService.refresh(user.getUsername());
    String token = jwtUtil.generateToken(user.getUsername(), permissions);
    return new LoginResponse(token, user.getUsername(), permissions);
}
```

**方案 C：事件发布改为异步（`@Async` + `@EnableAsync`）**

```java
// 步骤一：在 Spring Boot 启动类添加 @EnableAsync
@SpringBootApplication
@EnableAsync // P4-1：启用异步事件处理，防止事务内事件监听延长锁持有
public class Rxas400admApplication {
    // ...
}

// 步骤二：事件监听器改为异步执行
// [PermissionCacheEventListener.java]
@Component
@Async // ✅ 事件监听器在独立线程池中执行，不阻塞主事务
public class PermissionCacheEventListener {

    @EventListener
    public void onUserPermissionGranted(UserPermissionGrantedEvent event) {
        // 刷新 Caffeine 缓存（独立事务，不阻塞主流程）
        caffeineCache.invalidate("permissions:" + event.getUsername());
    }
}
```

---

### 11.4 纯查询方法审计：是否应移除 `@Transactional`

#### ✅ [良好实践] 所有纯查询方法均未添加 `@Transactional`

**审计结果**：经过全面扫描，项目中所有纯查询方法（SELECT only）均未添加 `@Transactional` 注解，这是正确的做法。

**正面教材清单**：

| Service | 纯查询方法 | 文件 |
|---------|-----------|------|
| `DocService` | `listTemplates()`, `listDocs()`, `versions()`, `detail()` | [DocService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/DocService.java) |
| `WebhookService` | `listAll()`, `logPage()`, `test()` | [WebhookService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/WebhookService.java) |
| `NotificationService` | `mine()`, `unreadCount()` | [NotificationService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/NotificationService.java) |
| `RegionService` | `page()`, `tree()` | [RegionService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/RegionService.java) |
| `NoticeService` | `page()` | [NoticeService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/NoticeService.java) |
| `DictService` | `listTypes()`, `listItems()`, `enabledItems()` | [DictService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/DictService.java) |
| `SysUserServiceImpl` | `listPermissions()`, `getByUsername()`, `listUsers()` | [SysUserServiceImpl.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/SysUserServiceImpl.java) |

**🔍 原因分析（AS400 特有优势）**：

1. 纯查询方法不加 `@Transactional` 意味着 Spring 不会为这些方法开启事务。在 AS400 环境下，这避免了 `SQL7008` 错误——因为纯查询不需要 Journaling，如果误加了 `@Transactional`，Spring 会尝试在查询结束后提交事务，而提交操作需要 Journaling 支持，如果表未开启日志就会报错。

2. 此外，`JTOpenAS400Client` 的 `queryList()` 方法使用 `try-with-resources` 管理连接，连接在方法返回后立即归还，不会持有事务锁。这是正确的 AS400 查询模式。

**唯一例外**：`SysUserServiceImpl.updatePassword()` 方法

```java
// SysUserServiceImpl.java
@Override
@Transactional // ⚠️ 这个是 UPDATE 操作，需要事务，但缺少 rollbackFor
public void updatePassword(String username, String encodedPassword) {
    userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
        .eq(SysUser::getUsername, username)
        .set(SysUser::getPassword, encodedPassword)
        .set(SysUser::getUpdatedTime, LocalDateTime.now()));
}
```

> **说明**：`updatePassword` 是写操作，事务是正确的，但需要补充 `rollbackFor = Exception.class`。

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——`updatePassword` 为裸 `@Transactional`（缺 rollbackFor）；「纯查询方法均未加 @Transactional」的结论经抽查成立。

---

### 11.5 AS400 事务审计总结与优先级清单

| 优先级 | 行动项 | 影响范围 | 预估工时 | 上线风险 |
|--------|--------|----------|----------|----------|
| 🔴 P0 | **运维执行 `STRJRNPF` 为所有 28 张核心表开启 Journaling** | 全量 28 张表 | 0.5 天（运维） | 未执行 = 所有事务回滚无效 |
| 🔴 P0 | **修复 14 个 Service 的 `@Transactional(rollbackFor = Exception.class)`** | 33 个方法 | 0.5 天（开发） | 低（纯注解修改） |
| 🔴 P1 | **拆分 `ReportScheduleService.execute()` 和 `JobScheduleService.execute()` 长事务** | 2 个方法 | 1 天 | 中（需回归测试） |
| 🔴 P1 | **拆分 `As400LoginService.login()` 长事务** | 1 个方法 | 0.5 天 | 中（认证流程变更） |
| 🟡 P2 | **配置 `@EnableAsync` + `@Async` 将事件监听器异步化** | 3 个 Service | 0.5 天 | 低（Spring 内置支持） |
| 🟡 P2 | **为 `JTOpenAS400Client` 显式设置 `transactionIsolation=none`** | 1 个类 | 0.25 天 | 低（仅影响查询） |
| 🟢 P3 | **添加 Checkstyle 规则检测 `@Transactional` 缺少 `rollbackFor`** | 构建配置 | 0.25 天 | 无 |

**本轮审计核心结论**：

1. **AS400 Journaling 是事务安全的基石**：项目中 28 张核心业务表需要在 AS400 端执行 `STRJRNPF` 命令开启 Journaling。如果运维未执行此步骤，所有 `@Transactional` 回滚将**静默失效**，数据一致性完全没有保障。这是上线前必须完成的 P0 级前置任务。

2. **`rollbackFor` 缺失是团队规范问题**：14 个 Service（占 70%）缺少 `rollbackFor = Exception.class`，而 6 个 Service 已经正确使用。这说明团队有规范意识但未统一执行，建议通过 IDE Live Template 或 Checkstyle 规则在代码提交阶段强制检查。

3. **长事务是 AS400 环境下的定时炸弹**：`ReportScheduleService.execute()` 和 `JobScheduleService.execute()` 在事务内执行 PDF 生成（10-30s）和远程 AS400 查询，在并发场景下会触发 `SQL0913` 锁超时。这两个方法必须优先重构。

4. **JTOpenAS400Client 的查询连接配置正确**：使用 `AS400JDBCDataSource` 独立连接，与 Spring 主数据源隔离，纯查询不走事务，避免了 `SQL7008` 错误。但建议显式设置 `transactionIsolation=none` 以明确意图。

> **AAA-Remark（2026-08-15 复核）**：✅ 结论成立。P0 项「STRJRNPF 28 表」与「修复 14 Service rollbackFor」可并行：前者是运维前置（当前 MySQL 环境不需要），后者是代码规范（随时可做）。优先级清单整体合理。

---

*本专项审计由 AS400/DB2 for i 部署环境驱动的深度事务安全审计，聚焦 Commitment Control 与 Journaling 机制。所有重构建议均已在代码中标注 `// P4-1` 等引用标记，优先级按 P0 > P1 > P2 > P3 排列。*
---
