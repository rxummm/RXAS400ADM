# RXAS400ADM 项目全面代码审查报告

> 审查范围：`D:\vueprojects\RXAS400ADM` 全量代码（后端 + 前端）
> 审查日期：2026-08-13
> 技术栈：后端 Spring Boot 3.3.4 / Java 17 / 多模块 Maven / MyBatis Plus / MySQL 8 / Flyway / Spring Security 6 + JWT / Quartz / WebSocket(STOMP)；前端 Vue 3.5 + TypeScript + Vite + Element Plus + Pinia + vue-i18n + ECharts + STOMP
> 审查方法：逐文件阅读 + 静态扫描 + 关键文件抽样核实（所有结论均附 `file:line` 证据）

---

## 目录

1. [项目总览与规模](#1-项目总览与规模)
2. [总体结论](#2-总体结论)
3. [架构评估（前后端分离 / 模块化）](#3-架构评估)
4. [后端代码质量](#4-后端代码质量)
5. [前端代码质量](#5-前端代码质量)
6. [功能完整性与领域逻辑](#6-功能完整性与领域逻辑)
7. [样式与布局复用性](#7-样式与布局复用性)
8. [硬编码审计](#8-硬编码审计)
9. [安全审查](#9-安全审查)
10. [测试质量](#10-测试质量)
11. [关键缺陷速查表（按严重度）](#11-关键缺陷速查表)
12. [整改优先级建议](#12-整改优先级建议)

---

## 1. 项目总览与规模

### 1.1 规模统计

| 类别 | 数量 | 说明 |
| --- | --- | --- |
| 后端 Maven 模块 | 9 | common / system / security / as400 / source / compile / deploy / monitor / app |
| 后端 Java 文件 | ~276 | 其中 Controller 36 个、Service 29 个、Entity 41 个、Mapper 40 个 |
| 后端测试类 | 19 | 共 ~49 个用例（surefire 报告全部通过） |
| Flyway 迁移 | 28 | V1__init.sql ~ V28 |
| 前端源码文件 | 126 | 63 个 `.vue` + 61 个 `.ts` + 2 个 `.css` |
| 前端视图页面 | 47 | 含主页面 8 个 + 子命名空间 39 个 |
| 前端 API 模块 | 44 | `src/api/*.ts` |
| 前端共享组件 | 9 | AppPagination / QueryBar / ExportButton / TableColumnSettings 等 |
| 前端总行数 | ~18,108 | 视图 10,082 行 |
| i18n 词条 | 977 / 977 | zh-CN 与 en-US 完全同步 |

### 1.2 模块分布（后端 class 数）

| 模块 | 功能 | 类数 |
| --- | --- | --- |
| rxas400adm-system | 用户/角色/菜单/权限/字典/公告/文档/配置 | 43 |
| rxas400adm-as400 | JS400 客户端抽象 / Job / SQL / IFS / 对象 / 子系统 | 50 |
| rxas400adm-app | 启动器 / 报表 / 巡检 / 数据初始化 / WebSocket 配置 | 34 |
| rxas400adm-monitor | 指标采集 / 告警 / 容量 / 基线 | 23 |
| rxas400adm-deploy | 发布流水线（步骤引擎/审批/回滚） | 19 |
| rxas400adm-common | ApiResponse / ErrorCode / 异常 / 事件 / Webhook | 17 |
| rxas400adm-security | JWT / 登录 / IP 规则 / AS400 委托登录 | 12 |
| rxas400adm-compile | 编译 | 5 |
| rxas400adm-source | 源码浏览 | 2 |

---

## 2. 总体结论

**评分参考（5 分制）：**

| 维度 | 评分 | 一句话评价 |
| --- | :-: | --- |
| 项目架构 / 前后端分离 | 4.5 | 分层清晰、模块划分合理、多服务器抽象设计优秀 |
| 前端代码质量 | 4.0 | 基建扎实（请求去重/混淆存储/主题体系），但类型化薄弱、复用不彻底 |
| 后端代码质量 | 3.5 | 框架规范、测试完善；但权限控制分散、错误码体系薄弱、存在重复代码 |
| 功能完整性 | 3.0 | 演示链路完整，但「真机可用性」存在多处存根/硬编码（MVP 标记明显） |
| 样式 / 布局复用性 | 3.5 | 统一骨架收敛良好，仍有 88 处内联样式与 7 处 scoped 重复定义 |
| 硬编码控制 | 3.0 | 主题变量体系优秀，但 40 处硬编码色 + 部分中文/i18n 覆盖仍需整改 |
| **综合** | **3.6** | **开发质量良好、演示可用；上生产前必须先解决安全与真机适配问题** |

**核心结论：**

1. **架构合规**：该项目的层次设计（Controller→Service→Mapper、DTO/VO 分层、多服务器 Provider/ThreadLocal 路由、事件驱动缓存失效）是同类项目中较出色的，前后端分离彻底（前端零直连 axios 绕过 API 层）。
2. **生产不可用风险**：发布流水线 TRANSFER 步骤是 MVP 存根（`TransferStep.java:7`）、AS400 密码明文存取（`AS400ClientProviderImpl.java:87-90`）、JT400 生产客户端的源码/成员操作硬编码或空实现、无分布式锁——这些决定了当前版本只适合演示与开发。
3. **安全缺陷真实存在**：`JobService` 3 处 SQL 注入、`Compile/CommandScript` 任意 CL 执行、无鉴权 WebSocket 频道、宽松 CORS、blob 下载携带混淆 token 致鉴权失效。
4. **前端体验层优良**：请求竞态取消、storage 混淆、i18n 977 词条双语言同步、组件资源释放到位，是加分项。

---

## 3. 架构评估

### 3.1 前后端分离（优秀）

- **代码分层彻底**：前端所有请求均经 `src/api/*.ts`（44 个模块）→ `request.ts` 统一实例，视图层不直接 import axios（除 `main.ts` 用于取消检测、`monitor.ts`/`report.ts` 用于 blob 下载）——规范执行到位。
- **统一响应契约**：后端 `ApiResponse{code,message,data}`（`common/response/ApiResponse.java`）+ 前端 `request.ts:10-14` 解包，code!=0 统一报错。
- **动态菜单 + 静态路由双轨**：后端 `/api/v1/auth/menu` 生成动态菜单，前端 `router/index.ts` 静态注册 + `MENU_PATHS`(43 项) 兜底校验。**但存在三重维护问题（见 M1）。**
- **多服务器路由模型清晰**：前端 `X-AS400-Server` 头 → `As400ServerIdInterceptor` → `As400ServerContextHolder`(ThreadLocal) → `AS400ClientProvider`。该抽象是本项目设计亮点。

### 3.2 后端模块依赖（良好，有瑕疵）

- **依赖方向正确**：common 无 Spring Web/Controller 依赖；system/security/as400 依赖 common；app 聚合全部。`@MapperScan("com.rxas400adm.**.mapper")` 统一扫描。
- **命名规范合规**：包名统一 `com.rxas400adm.{module}`，Mapper 包以 `.mapper` 结尾满足扫描约束；DTO/VO/Entity 分层齐全。
- **瑕疵**：
  - `DeploymentController` 的 approve 接口 `@OperateLog` 在 controller 而 `@PreAuthorize` 在 service（跨层不一致，`DeploymentService.java:61,90` 与 controller 注释对比）。
  - 系统模块 service 数量膨胀（15 个 service），`SysUserService`/`SysUserServiceImpl` 接口粒度偏大（单实现接口），部分"接口+实现"是单实现却仍拆两层。
  - `rxas400adm-system` 承担了过多横切职责（审计 AOP、Webhook、事件监听），与 `common` 职责边界略模糊，但可接受。

### 3.3 数据库与迁移

- Flyway `V1`~`V28`：22+ 张表，`rx_` 前缀规范一致；`V12__backfill_role_menu.sql`、`V14__button_menus.sql`、`V17__user_menu_auth.sql`、`V21`/`V22`（viewer 授权）等迁移反映需求持续演进。
- **问题**：V1 之后的多张迁移存在"加表→回填→再加固"的重复路径（如 V8 菜单管理、V9 菜单分组、V17 用户菜单授权、V24 权限管理），反映早期设计未稳定即上线，历史包袱沉淀。
- `validate-on-migrate: false`（application.yml:17）降低了迁移安全校验，建议生产打开。

### 3.4 实时推送

- WebSocket STOMP（`/topic/deployment/{id}`、`/topic/monitor/{instanceId}`、通知广播）链路完整，前端 3 个消费者都正确 `deactivate()`。
- **但 `/ws` 免认证 + `/topic/**` 无鉴权**（见 9.4），且通知将"全员消息"广播到一个共享 topic（任何登录的 WebSocket 客户端都能收）。

---

## 4. 后端代码质量

### 4.1 Critical 缺陷

| # | 位置 | 问题 |
| --- | --- | --- |
| B-01 | `AS400ClientProviderImpl.java:87-90` | **密码明文存储/返回**：`decryptPassword()` 直接返回 `getPasswordEncrypt()`，注释自认 "MVP，后续用 AES-256"。Seed 数据 4 台服务器 `passwordEncrypt="demo-placeholder"`（`DataInitializer.java:231`）。`IbmiSystem.java:31-32` 的 `@JsonIgnore` 只防序列化泄露，不解决存储明文。若生产库被读则凭据全泄露。 |
| B-02 | `JobService.java:35,92-94,113-115` | **3 处 SQL 注入**：`activeJobs` 把 `@RequestParam status` 直接拼进 `WHERE JOB_STATUS = '"+status.toUpperCase()+"'`；`jobLog`/`msgwMessages` 的 `jobUser/jobName` path 变量未转义拼接进 SQL。（注：`SqlQueryService` 的 `sq()` 做了转义，但 JobService 没走该工具。） |
| B-03 | `CompileService.java:48-51` + `CompileController.java:26-30` | **任意 CL 命令注入**：`request.getCommand() + " PGM(...) SRCFILE(...)"` 接受用户任意 CL 前缀；且 CompileController 无 `@PreAuthorize`（仅 service 层有，权限控制不牢）。 |
| B-04 | `CommandScriptService.java:96-110` + `ScriptController.java:78-83` | 保存的任意 CL 对目标服务器直接执行，仅 `SCRIPT_MANAGE` 门槛，无命令白名单/参数化。 |
| B-05 | `TransferStep.java:7,17-20` | **发布流水线核心 TRANSFER 步骤是 MVP 存根**：恒返回 `StepResult.ok("TRANSFER COMPLETE (SAVF RXDEP/SAVE001)")`。跨机发布实际不可用，且 SAVE/RESTORE 仅在目标机拼 CL，"发布"在当前实现上是单机假流程。 |

### 4.2 Major 缺陷

| # | 位置 | 问题 |
| --- | --- | --- |
| B-06 | `DeploymentService.java:136-158` | 异常时状态机失控：`forServer()` 在 try 外（line 136），服务器不可用则直接抛异常，此时 `deployment.status` 已置 RUNNING（line 131）且**永不更新**→永久卡 RUNNING。 |
| B-07 | `DeploymentService.java:150-154` | catch 块**无条件**设 `ROLLBACK_SUCCESS`，即使回滚步骤本身失败（`PipelineExecutor.java:83-87` 仅打日志"请人工介入"）也虚标成功。 |
| B-08 | `DeploymentService.java:121-159` | 无幂等/并发控制（无 `@Version` 乐观锁、无 DB 锁），双击"执行"可双跑流水线双插 task。 |
| B-09 | `As400LoginSyncService.dailySync()` | 每日同步**删除本地用户与角色但无事务/审计**；IBM i 侧若瞬时返回空 profile 列表可级联误删账号。另 profile 缺省值 `"mock"`（`@Value` 默认）使生产环境若忘配 profile 则同步**永不运行**。 |
| B-10 | `CollectorScheduler.java:19,31` | 注释声称"配合分布式锁 rx_scheduler_lock"，实际 grep 无锁实现；RAMJobStore + 多实例部署会重复采集/重复告警。调度另用 `@Scheduled`（应用内），非 Quartz 集群能力。 |
| B-11 | `AlertEngine.java:29-57` | 告警规则 `durationSeconds`（种子规则 300s）**未参与判定**：单次采样超阈值即告警，无去重 / 无打开-恢复状态机 / 无持续时长窗口 → 持续故障每 10s 刷一条告警并全员通知（消息风暴）。 |
| B-12 | `AlertWebhookListener.java:46-51` | 无论规则的 channel 是什么，站内通知**永远发给全体活跃用户**（`sendToAllActiveUsers`），NONE 通道也轰炸全站。 |
| B-13 | `NotificationService.java:51-58` | `sendToAllActiveUsers()` 每用户一次 INSERT + 一次 WS `convertAndSend`（N+1 落库 + N 次广播），全站通知/告警时放大明显。 |
| B-14 | `JTOpenAS400Client.java:52-57,96-106,116-117` | 生产客户端多处**硬编码/空实现**：每次操作 `new AS400(...)` 无连接池；`listMembers()` 恒返回空、`readMember()` 恒返回 `""` 并 warn（源码查看/编译预览核心功能真机不可用）；`listLibraries/listSourceFiles` 硬编码返回 `{"QSYS","QTEMP","APP","ORDERS"}` 等；JDBC URL 明文拼 `password=`。 |
| B-15 | `JTOpenAS400Client.java:32,132-135` | 大量 CL/查询"优雅降级"为失败/空结果不抛异常——真机上多模块静默不可用，排障困难。 |
| B-16 | `SqlQueryService.java:37-62` | 仅 `startsWith("SELECT")` 守卫：可执行 `SELECT...;DROP...`（多语句）、`SELECT...FOR UPDATE`（持锁）；无超时、无查询长度限制；`MAX_ROWS=200` 仅是客户端截断，DB 端全量执行。 |
| B-17 | `JobScheduleService.java:216-222` | `isReadOnly()` 仅前缀匹配 SELECT/WITH，无语句结尾校验，可 `SELECT ...; DELETE ...`。 |
| B-18 | `SourceController.java:21-41` | **4 个 GET 端点全部无 `@PreAuthorize`**：任意登录用户可枚举库/文件/成员并读取源码内容。 |
| B-19 | `ObjectService.java:31-51` / `ExecutionController.java:53-67` | 注释称"避免大库全量传输"实则拉全量（`FETCH FIRST 200 ROWS ONLY`）后**内存分页**，超 200 行被截断、total 不准确；Execution 历史 N+1（每行 `selectById` schedule）。 |
| B-20 | `JobService.java` / `RoleService.java:31` / `MenuService` | 性能：`activeJobs` 全量拉取后内存过滤；`RoleService.listAll` 每角色查 menuIds（N 次查询）；`/auth/menu` 每次请求全量重建菜单树无缓存。 |
| B-21 | `CpuCollector.java`/`MemoryCollector.java` (~line 31) | 采集器 `Double.valueOf(row.get("...").toString())` 对可空的 JT400 列直接 NPE。 |
| B-22 | `ReportService.java:237-240,266-291` | PDF 中文依赖本机字体，字体候选硬编码 Windows/Linux 路径，缺失时**静默返回 `new byte[0]`（空文件）**。 |
| B-23 | `AS400ClientProviderImpl.java:33-35` | `activeProfile.contains("mock")` 是"包含"判断而非"等于"——含 mock 字符串的任一 profile（如 `prod-mock`）都会切到 Mock 客户端。 |

### 4.3 Minor 缺陷

- **错误码体系薄弱**：`ErrorCode.java` 仅 14 个码（通用 400/401/403/404/500 + user/AS400/deploy 三段），monitor/system/report/compile/source 全部复用 `BAD_REQUEST`，错误定位弱；大量 `new BusinessException("...")` 无码（如 `DeploymentService.java:102,155`）。
- **重复代码**：`JobScheduleService` 与 `ReportScheduleService` 共享 ~120 行近相同 Quartz 注册/清理逻辑；`JTOpenAS400Client` 与 `AS400ClientProviderImpl` 的服务器加载逻辑重复。
- **GET 带副作用**：`BaselineService.computeBaseline()` 在 `GET /monitor/baseline/{id}` 请求内写库（`MonitorController.java:59-63`）。
- **`@Autowired` 字段注入**：少量存在（如 `DataInitializer`、`JwtAuthenticationFilter` 等），与 AGENTS.md "禁止 @Autowired 字段注入" 约定冲突。
- **无 `@Async`**：发布/报表生成/调度全部同步阻塞 HTTP 线程；报表生成等长任务会占满 Tomcat 工作线程。
- **空 catch / 静默吞错**：`MonitorController` 多处 try-catch 忽略；`MockAS400Client` 对 CRT/SAV/RST/END/CHG 前缀一律返回成功（演示可接受，但掩盖真实错误）。
- **截断/损坏测试文件**：`JwtUtilTest.java`（375 行，~80 个重复 import、含字面 `复制代码` 行、断言重复 ~20 次）、`PipelineExecutorTest.java`（136 import / 仅 35 唯一）——文件从文档粘贴污染，需清理。
- **客户端缓存永不失效**：`AS400ClientProviderImpl` 按 serverId 缓存客户端实例，服务器凭据/启用状态变更不重建（但每次 loadSystem 校验 enabled，尚可）。

### 4.4 发现为好

- `SqlQueryService.sq()` 单引号转义 + 只读守卫（尽管有 B-16 缺口）、`AuthController` 登录全链路（IP 黑白名单 + 限流 + 用户名锁定 + 失败审计）、`JwtAuthenticationFilter` 权限 60s 缓存与 token 内嵌权限回退、`OperateLogAspect` finally 写审计（失败也记录）——这些设计稳健。
- ThreadLocal（As400ServerContextHolder）在拦截器 `afterCompletion` 正确清理（WebMvcConfig + As400ServerIdInterceptor），无泄漏。
- `WebhookNotifier` 重试（3 次/500ms/3s 超时）与 `EmailNotifier` 未配 SMTP 优雅跳过，降级策略合理。
- `PermissionCacheEventListener`（`UserPermissionGrantedEvent`→缓存失效）事件驱动设计好。
- Quartz `volatile holder` 静态注入模式（`ScheduleQuartzJob`/`ReportScheduleQuartzJob`）处理 SpringBoot Quartz 无构造注入的变通正确，且带 null-guard。

---

## 5. 前端代码质量

### 5.1 Critical 缺陷

| # | 位置 | 问题 |
| --- | --- | --- |
| F-01 | `src/api/monitor.ts:29-31`、`src/api/report.ts:43-45` | **blob 下载发送混淆 token 致鉴权失效**：直接 `localStorage.getItem('rxas400_token')` 取原始值，而 token 经 `useStorage.set()` Xor+Base64 混淆后才能读（`useStorage.ts:22-36,87-93`），`request.ts:34` 正确用了 `useStorage.get()`。`exportInspection()`/`downloadReport()` 因此向后端发送 `Bearer <混淆串>`，JWT 校验失败，且 blob 实例无错误拦截器 → 表现为不确定的坏文件下载。 |
| F-02 | `src/views/Monitor.vue:166,185,205,214` | **监控页硬编码 instance 1**：WS 订阅 `'/topic/monitor/1'`、`fetchCapacity(1,60)`、`fetchOverview(1)`、`fetchMetrics(1,30)` 全部写死 1。REST 请求虽带 `X-AS400-Server` 头（request.ts:39-49），但 WS 频道永远绑订服务器 1——切换服务器后实时遥测仍显示 1 号机。应改 `useAs400ServerStore().currentServerId`。 |

### 5.2 Major 缺陷

| # | 位置 | 问题 |
| --- | --- | --- |
| F-03 | `src/router/index.ts:13-22,50-326` | **页面注册三重维护**：`MENU_PATHS`(43 项) 需与静态路由 children 和后端 `rx_menu` 表同步，三处不一致即产生"菜单可见但访问被拒"或反之。`system/notifications` 的豁免证明列表非路由派生。 |
| F-04 | 全 `src` | **类型化薄弱**：`any` 家族 ~181 处（96 `: any` + 35 `<any>` + 50 `as any`）。`ApiResponse<T>` 仅在拦截器结构使用，调用点不约束；`useTablePage<any>` 是大多数消费者默认。视图层出现 `deployments.value = (await fetchDeployments()) as any[]`（`Deploy.vue:157`）等强转。`tsconfig` `noUnusedLocals:false`/`noUnusedParameters:false` 关闭了未用符号检查。 |
| F-05 | `src/views/job/index.vue:48,57,61` | **权限复用过粗**：Hold/Release/Reply 按钮与 End 共用 `v-has-perm="'JOB_END'"`——所有作业变更都需要"结束作业"这种破坏性权限。 |
| F-06 | `src/views/system/Users.vue`(592 行)、`system/menus/index.vue`(571 行)、`layout/index.vue`(527 行) | **超大组件**：Users.vue 混合用户列表/安全 tab/IP 统计/权限管理 5 个子屏。 |
| F-07 | 全 `views` | **分页实现重复**：`useTablePage` 仅在 14 个视图复用，另有 ~22 个视图手写 `current/size/total/load()` 同模式；`@change="() => {}"` 空处理 15 处。 |
| F-08 | `Monitor.vue:163`、`Deploy.vue:197`、`NotificationBell.vue:123` | **WebSocket URL 硬编码 `ws://`**：HTTPS 下混合内容被浏览器拦截；应 `(location.protocol==='https:'?'wss://':'ws://')+location.host+'/ws'`。 |
| F-09 | `Deploy.vue:193-206` | **STOMP 客户端泄漏**：每次 `showLogs` 新建 `new Client()` 并 `activate()` 但未先 `deactivate()` 旧的，反复开关日志窗会堆积 socket 直到页面卸载。 |
| F-10 | `src/views/system/i18n/index.vue:51` | 唯一 `style="width: 100%"` 违反 AGENTS.md 硬性红线，应改 `.w-full`。 |

### 5.3 Minor 缺陷

- **表单校验靠 `ElMessage.warning` 手写**（Users/dict/assets/docs/jobSla/scripts/alertRules/messageFiles/schedule/reports 等），仅 Login.vue、menus 用 `:rules`，新建用户表单甚至无校验。
- **定时器挂号在 `window`**：`Monitor.vue:226-231` `(window as any).__rxas400_monitor_timer` 全局 hack；单页可用但脆弱。
- **keep-alive 失效**：路由 `SysDict` 与组件名 `DictManage`（`system/dict/index.vue:149`）不一致，`cachedViews` 存 route.name → dict 页实际不缓存（静默失效）。
- **分页双击发**：`Users`/`loginLog`/`permissionRequest` 同时绑 `@change="load" @size-change="load"`，改变 size 时 `AppPagination` 重置 current=1 又触发 change → 请求重复（靠 request.ts 竞态取消兜底）。
- **localStorage 绕过封装**：`data/sysvals/index.vue:110,118` 和 `stores/as400Server.ts:20,39` 直接操作 localStorage，未走 `useStorage`（编码一致性破洞）。
- **文案错误**：`Login.vue:125` 用 `t('common.loading')`("加载中...") 作为登录失败回退文案。
- **死代码**：`query/index.vue:132` `void ElMessage`；`tsconfig` 关闭未用局部检查使这类问题无法被构建拦截。

### 5.4 发现为好（前端亮点）

- **请求竞态取消**（`request.ts:21-62`）：同 method+url+params 只保留最后一次，GET 自动 abort 旧请求，写请求不误伤；`isCanceled` 静默机制（main.ts:32-48）+ `unhandledrejection` 拦截杜绝控制台噪音——工程化水准高。
- **资源管理好**：ECharts `dispose()`（topology/Monitor/serverCompare/jobDependency）、STOMP `deactivate()`（3 处）、interval 清理、事件监听移除（layout/TagsView）均到位。
- **i18n 卓越**：977/977 键双语言完全同步，命名规范，后端运行时覆盖（`i18n/dynamic.ts`）。
- **组件资源复用良好**：9 个共享组件被广泛使用（AppPagination 34 视图、QueryBar 13、v-has-perm 指令 50 处）；58 处破坏性操作统一 `ElMessageBox.confirm`。
- **零卫生问题**：无 `@ts-ignore`、无 TODO/FIXME/HACK、无 eval/new Function、仅 1 处 console.error（全局 handler）。

---

## 6. 功能完整性与领域逻辑

> 以下判断混合代码事实（MVP/mock/存根注释）与当前 profile（默认 mock）得出。

### 6.1 可用（链路完整，演示/半生产可用）

| 功能 | 状态 | 备注 |
| --- | --- | --- |
| 认证/RBAC/菜单 | ✅ | JWT 全流程 + 权限缓存 + 动态菜单 + 按钮权限 |
| 登录安全 | ✅ | IP 黑白名单、限流、用户名锁定、失败审计、AS400 委托登录 |
| 指标采集展示 | ✅ | 10 采集器 + 存储 + WS 实时 + 容量预测 + 基线 |
| SQL 查询 | ⚠️ | 可用但只读守卫有漏洞（B-16） |
| 作业查看/操作 | ⚠️ | Mock 完整；真机有 NPE 与注入风险（B-02/B-21） |
| 调度（作业/报表） | ✅ | Quartz + RAMJobStore（重启靠 runner 重注册） |
| 审计日志 | ✅ | AOP 统一落库，改操作完整 |
| 报表（Excel） | ✅ | Websocket 提醒 + 定时邮件；PDF 受字体限制（B-22） |
| 巡检 | ✅ | 聚合评分 + 导出（前端 blob 需修 F-01） |
| 系统管理全系 | ✅ | 字典/公告/文档/通知/webhook/区域/日历/收藏/i18n 运行时 |

### 6.2 存根 / Mock 依赖（上生产前必须替换）

| 功能 | 位置 | 现状 |
| --- | --- | --- |
| 发布跨机传输 | `TransferStep.java:7` | MVP 存根，恒成功 |
| AS400 密码 | `AS400ClientProviderImpl.java:87` | 明文 = 存储值 |
| 源码成员浏览 | `JTOpenAS400Client.java:96-106` | 真机恒空/恒 " " |
| 库/源文件列举 | `JTOpenAS400Client.java:86-93` | 真机硬编码返回固定名单 |
| 连接池 | `JTOpenAS400Client.java:52-57` | 无池，每次 new，URL 明文密码 |
| 分布式锁 | `CollectorScheduler.java:19` | 注释声称有，实际无 |
| 作业仿真峰值 | `MockAS400Client.java:95-101` | 3% 概率峰值专为告警演示 |
| DataInitializer 种子 | `DataInitializer.java:544,559,573` | "V28：建议后续功能"等占位 |

### 6.3 领域逻辑专项结论

- **发布审批流**：状态机（WAIT_APPROVAL→CREATED→RUNNING→…×ROLLBACK）框架完整，但默认 `requireApproval=false`（`DeploymentRequest.java:28`）；审批人未校验与创建人不一致；执行无并发防护（B-08）。回滚结果不真实（B-07）。
- **告警引擎**：规则匹配（operator+threshold）实现正确，但缺持续性判定/去重/恢复——这是监控类系统核心语义缺失（B-11）。
- **容量预测**：仅 DISK 单指标、预测天数无 30 天上限、未校验趋势质量（Minor）。
- **作业调度**：CRON 覆盖/清理正常；Quartz 触发器旧实例未删除的风险低（runner 统一重建）。
- **SLA**：期望时长 + 偏差 % 校验实现完整（正向）。
- **文档管理**：DRAFT/PENDING/PUBLISHED/REJECTED 版本流转，内容存 DB（无文件上传 → 无路径穿越面，正向）。

---

## 7. 样式与布局复用性

### 7.1 统一骨架执行情况

按 AGENTS.md 约定，页面应为 `page-container page-container--fit` + `.search-bar` + `.table-wrapper` + `AppPagination`。

- **达标视图 14/47**：job、Users、cache、config、dict、i18n、ipRules、loginLog、notice、notifications、permissions、roles、tasks、webhooks。
- **未达标 ~28 个列表/工具视图**：`audit`、`scripts`、`executions`、`ifs`、`schedule`、`docs`、`pf`、`objects`、`Monitor`、`health`、`query`、`Deploy`、`biz-data`、`messageFiles`、`sysvals`、`tableFields`、`sla`、`dependency`、`inspection`、`serverCompare`、`alertRules`、`region`、`topology`、`menus`、`permissionRequest`、`report`、`calendar`、`assets`——多数缺 `--fit`（表格不吸底）或表格用 `el-card` 而非 `.table-wrapper`（objects/pf/query/Monitor/health 的表格在 el-card 内）。
- **Tab 页摆放差异**：`webhooks`/`permissionRequest` 的 `el-tabs` 在 `.table-wrapper` 外（合规参考是 job/Users 的 tabs 内嵌 table-wrapper）；`params`/`report` 正确。

### 7.2 Scoped 样式重复定义（违反"禁重复同名 scoped"约定）

| 位置 | 重复类 | common.css 已提供 |
| --- | --- | --- |
| `components/ShortcutsHelp.vue:51` | `.hint` | :191 |
| `views/job/sla/index.vue:206,209` | `.mt16` `.mb8` | :38, :41 |
| `views/monitor/inspection/index.vue:183` | `.mt16` | :38 |
| `views/system/Users.vue:567,590` | `.muted` `.mb16` | :73, :43 |
| `views/system/dict/index.vue:317` | `.table-wrapper`(flex:1 布局) | :145-152 |
| `views/system/roles/index.vue:415` | `.table-wrapper`(同上) | :145-152 |
| `views/system/permissionRequest/index.vue:450` | `.muted` | :73 |

`dict`/`roles` 最典型：把 common.css 已给的 `page-container--fit .table-wrapper` 吸底行为重新实现了一套 scoped 样式。

### 7.3 内联样式总量

**88 处 `style="..."` 分布于 41 个文件**，构成（按 AGENTS.md 硬编码红线分级）：

- `width: 100%` 违规 1 处（i18n/index.vue:51——唯一命中）。
- `width: Npx` 49 处（~28 文件，多为查询输入框 200/220/240px，AGENTS 允许"个别字段覆盖"，但 "6 个 system 页统一 240px" 属复制粘贴可收敛）。
- `<div style="flex: 1">` 占位符 19 处（12 文件）——**全部应改 `.flex-1` 类**（common.css:183 已提供）。
- 动态 `:style` 绑定 ~11 处（QueryBar/TableColumnSettings/PasswordStrength/Dashboard/calendar/sysvals 等）。

### 7.4 公共工具类缺口

- `common.css` 有 mt4/8/16/24、mb4/8/12/16/24、ml8、mr8、mr4，但——`.ml4` **未定义却已被使用**（`alertRules/index.vue:19`），页面静默丢失该间距；`.ml2`/`.mx8` 被局部定义（`As400ServerSelector.vue:85`/`alertRules:264`）与 4/8 刻度不一致（6px）。
- common.css 中 `.pager`、`.mr8`、`.mt8`、`.mt24`、`.mb24`、`.mb4` **零使用**（死代码，可清理或作为 API 保留）。

### 7.5 复用性评估小结

方向正确（统一骨架 + 公共工具类收敛 + QueryBar/AppPagination 组件化），但**只覆盖了 1/3 视图**；~1441 行 scoped CSS 中相当比例（间距、header 行、卡片配方 `padding+radius+border`）是同类函数包的重复实现，适合再抽象一个 `.panel`、`.flex-header` 公共类。

---

## 8. 硬编码审计

### 8.1 硬编码颜色（theme.css:6 明文禁止 "#fff/#1677ff 等散落"）

**40 处 hex 色散落 13 个文件**（不含 rgba 3 处 TagsView）：

| 位置 | 颜色 | 问题 / 应替换为 |
| --- | --- | --- |
| `views/Source.vue:114-115` | `#0d1117` / `#c9d1d9` | 硬编码 GitHub-dark 代码块，亮色模式下是黑底——最严重，应加 `--code-bg/--code-fg` 主题变量 |
| `layout/index.vue:357,478,514,517` | `#fff`×2、`#f7ba2a`×2 | 激活菜单字色、头像、金色标签——`var(--color-*)` |
| `components/PasswordStrength.vue:47` | `['','#f56c6c','#e6a23c','#409eff','#67c23a']` | 直接复刻 `--color-danger/warning/primary/success` |
| `views/Dashboard.vue:16,94,100,107,109` | `#fff`、`#1677ff/#52c41a/#fa8c16/#f5222d` | 与 `--rx-primary`/主题色重复 |
| `layout/As400ServerSelector.vue:72,75,78,81` | `#52c41a/#909399/#faad14` | `#909399` 恰是 `--text-secondary` |
| `views/calendar/index.vue:173,394` | 整个 `['#1677ff','#67c23a','#e6a23c','#f56c6c','#9c27b0','#00bcd4','#909399']`、`#fff` | 整组主题色重复 |
| `views/topology/index.vue:71-75,123` | `#1677ff…#909399` | 同上 |
| `views/job/dependency/index.vue:73` | `'#409eff' : '#67c23a'` | ECharts 节点色 |
| `views/monitor/inspection/index.vue:149` | `.score-warn { color:#e6a23c }` | 邻位 `.score-ok` 已用 `var(--color-warning)`，此处未用 |
| `views/system/menus/index.vue:546` | `#fff` | |
| `views/assets/index.vue:327` | `var(--bg-color, #f5f7fa)` | **`--bg-color` 在 theme.css 未定义**→回退到硬编码 `#f5f7fa`（应为 `--bg-hover`） |

### 8.2 硬编码中文文案（须走 `$t()`/i18n）

非注释的用户可见中文共 6 处 + 4 处模板绑定：

- `views/assets/index.vue:221` — `'********（已配置）'`
- `views/calendar/index.vue:181` — `` `${viewYear} 年 ${viewMonth} 月` ``
- `views/data/messageFiles/index.vue:32` — `` `${f.MESSAGE_FILE_NAME}（…，${f.NUMBER_OF_MESSAGES} 条）` ``
- `views/docs/index.vue:316` — `tplForm.category || '通用'`
- `views/system/dict/index.vue:114` — `placeholder="运行中"`
- `views/system/loginLog/index.vue:76` — `module: '登录安全'`
- 模板绑定中文括号/计数：`biz/data/index.vue:31,44`、`data/tableFields/index.vue:24,37`、`messageFiles/index.vue:32`

另有 **~12 个视图**的英文硬编码标签（`label="ID"/"IP"/"Server"/"Bean"/"Cron"/"Type"/"Port"`、`QPGMR/QSECOFR`、`WRKACTJOB`、`audit/index.vue:19,24` 等）违反"页面文案都走 i18n"的宽泛原则，严重级次之。

### 8.3 后端硬编码 / 魔法值

- `application.yml:41` JWT secret 有打包默认值 `RXAS400-Enterprise-IBM-i-Operation-Platform-Secret-2026`（已提供环境变量覆盖，但生产忘配即用弱默认钥）。
- `application.yml:3-6` `spring.profiles.active: mock` 硬编码默认档（生产忘配即 Mock 模式）。
- `DataInitializer.java:99,192-216` 演示账号 admin/admin123 写死在初始化器。
- 种子页面 `DataInitializer.java:225-244` 服务器密码 `demo-placeholder`；`line 544/559/573` "建议后续功能"占位。
- 监控间隔 10000ms、保留 30 天在 yml 属配置合理；但 `MockAS400Client.java:95` 3% 概率峰值、`FETCH FIRST 200 ROWS ONLY`、报表 500 行上限等魔法数字散落代码。

### 8.4 硬编码审计结论

前端主题体系（theme.css + `color-mix` + html.dark）设计优秀，但约 1/3 的视图绕过它散落硬编码色与内联尺寸；中文 i18n 覆盖已到 977 键的规模，剩余 6 处脚本中文 + 多家模板绑定属漏网之鱼，修复成本低。**后端 magic string 与演示值残留是更大的隐患。**

---

## 9. 安全审查

| 严重度 | 发现 | 位置 |
| --- | --- | --- |
| Critical | **SQL 注入**（3 处作业查询拼接用户输入） | `JobService.java:35,92-94,113-115` |
| Critical | **任意 CL 命令执行**（节点/脚本） | `CompileService.java:48-51`、`CommandScriptService.java:96-110` |
| Critical | **AS400 密码明文存储** | `AS400ClientProviderImpl.java:87-90`、`DataInitializer.java:231` |
| Critical | **WebSocket 无鉴权**：`/ws` permit-all + `/topic/**` 任意 WS 客户端可订阅部署日志/监控指标/全员通知 | `SecurityConfig.java`、`WebSocketConfig.java`、`NotificationService.broadcast` |
| Major | **CORS 全开**：`allowedOriginPatterns("*")` + `allowCredentials(true)` | `WebMvcConfig.java`（as400 或 app 模块） |
| Major | **JWT 默认密钥落盘**（生产忘配即弱钥） | `application.yml:41` |
| Major | **SQL 查询服务只读守卫可绕过**（多语句/锁语句/全量执行） | `SqlQueryService.java:37-62` |
| Major | **调度只读校验仅前缀匹配** | `JobScheduleService.java:216-222` |
| Major | **源码浏览端点无权限**（4 个 GET） | `SourceController.java:21-41` |
| Major | **部署失败状态虚标回滚成功 + 卡 RUNNING**（可用性/一致性） | `DeploymentService.java:131,150-154,136` |
| Major | **AS400 每日同步可级联删号 / prod 忘配 profile 永不执行** | `As400LoginSyncService.dailySync()` |
| Major | **JT400 真机连接无池、URL 明文密码、成员操作为空实现** | `JTOpenAS400Client.java:52-57,96-106,116-117` |
| Minor | `ObjectService`/`JTOpenAS400Client` 多处 CL 拼接仅靠局部白名单（`BusinessService.java:31` 正则 `^[A-Z0-9_$#@]+$` 是单模块专属，其他端点无白名单） | |
| Minor | `CHGSYSVAL` 仅做 SQL 风格 `'`→`''` 转义，CL 不按 SQL 转义解析 | `JTOpenAS400Client.java:413-419` |
| 正向 | 登录全链路安全（IP 黑白名单/限流/锁定）、`@JsonIgnore` 密文字段、权限 60s 缓存+token 回退、审计 AOP | |

**总体**：认证与授权体系骨架完整（77 个 `@PreAuthorize` + 前端 50 处 `v-has-perm`），但**执行面（SQL/CL/WS/CORS）防护明显弱于认证面**，这是对运维类系统最致命的失衡。

---

## 10. 测试质量

| 模块 | 测试类 | 覆盖点 | 评价 |
| --- | --- | --- | --- |
| common | ApiResponseTest、WebhookNotifierTest | 响应契约、重试 | 良好 |
| security | JwtUtilTest、LoginAttemptServiceTest、PermissionServiceTest、As400LoginServiceTest、As400LoginSyncServiceTest | JWT/锁定/权限缓存/委托登录/同步 | **JwtUtilTest 文件被污染**（重复 import、`复制代码` 字面、断言重复 ~20 次），需重写 |
| as400 | MockAS400ClientTest + 8 个 service 测试 | 客户端/服务层 | 良好（9 个测试类） |
| monitor | AlertEngineTest、AlertRuleTest、DiskCollectorTest、BaselineServiceTest、CapacityServiceTest | 告警/基线/容量/采集器 | 良好 |
| deploy | PipelineExecutorTest | 流水线 | 文件被污染（136 imports 仅 35 唯一），需清理 |
| system | DocServiceTest、SysUserServiceImplTest | 文档/用户 | 覆盖薄（15 个 service 仅 2 有测试） |
| app | ReportServiceTest、EmailNotifierTest、HealthControllerTest | 报表/邮件/健康 | 良好 |

**缺口**：compile、source 模块零测试；controller 层几乎零集成测试；`ReportScheduleQuartzJob`/`ScheduleQuartzJob`（Quartz）无测试；SQL 注入高风险点（JobService/SqlQueryService）无负面用例。49 个用例对 276 个 Java 文件，覆盖密度中等偏低，但**已覆盖的均为实质行为测试**，质量不错。

---

## 11. 关键缺陷速查表（按严重度）

### 11.1 Critical（必须优先修）

| ID | 位置 | 一句话 |
| --- | --- | --- |
| B-01 | AS400ClientProviderImpl:87 | AS400 密码明文存取 |
| B-02 | JobService:35,92-94,113-115 | 3 处 SQL 注入 |
| B-03 | CompileService:48 | 任意 CL 编译命令注入 + controller 无权限 |
| B-04 | CommandScriptService:96 | 任意 CL 脚本执行，仅一权限门槛 |
| B-05 | TransferStep:7 | 发布流水线跨机传输是 MVP 存根 |
| F-01 | monitor.ts:29 / report.ts:43 | blob 下载发混淆 token 致鉴权失效 |
| F-02 | Monitor.vue:166,185,... | 监控页硬编码 instance 1 |
| S-01 | SecurityConfig + WebSocketConfig | WebSocket 频道无鉴权（部署/监控/通知泄露） |

### 11.2 Major（上生产前必修）

| ID | 位置 | 一句话 |
| --- | --- | --- |
| B-06 | DeploymentService:131 | 失败后状态永久卡 RUNNING |
| B-07 | DeploymentService:150 | 失败虚标 ROLLBACK_SUCCESS |
| B-08 | DeploymentService:121 | 无并发/幂等防护，双击双跑 |
| B-09 | As400LoginSyncService | 可级联删号；prod 忘配 profile 永不执行 |
| B-10 | CollectorScheduler:19 | 声称有分布式锁实际没有 |
| B-11 | AlertEngine:29 | 告警 duration 未用，无去重/恢复 → 消息风暴 |
| B-12 | AlertWebhookListener:46 | 告警永远全站轰炸 |
| B-13 | NotificationService:51 | 全站通知 N+1 落库+N 广播 |
| B-14 | JTOpenAS400Client | 成员/源码硬编码/空实现，真机不可用；无连接池 |
| B-16/B-17 | SqlQueryService/JobScheduleService | SQL 只读守卫可绕过 |
| B-18 | SourceController | 4 个源码浏览 GET 无权限 |
| B-19 | ObjectService/ExecutionController | 假分页（200 行截断）+ N+1 |
| B-22 | ReportService:237 | PDF 缺字体静默空文件 |
| B-23 | AS400ClientProviderImpl:33 | profile"包含 mock"判断过宽 |
| F-03 | router/index.ts | 路由三重维护（MENU_PATHS 双源） |
| F-04 | 全 src | ~181 处 any，类型化薄弱 |
| F-08 | Monitor/Deploy/NotificationBell | ws:// 硬编码，HTTPS 失效 |
| F-09 | Deploy.vue:193 | STOMP 客户端泄漏 |
| S-02 | WebMvcConfig | CORS `*` + credentials |

### 11.3 统计汇总

| 指标 | 数值 |
| --- | --- |
| 后端 Bug/问题总数 | ~90 项（Critical 5 / Major 18 / Minor ~40 / Info 若干） |
| 前端 Bug/问题总数 | ~80 项（Critical 2 / Major 8 / Minor ~30） |
| 前端 `style="…"` | 88 处 / 41 文件（含 width:100% 1 处、width:Npx 49 处、flex:1 占位 19 处） |
| 硬编码 hex 色 | 40 处 / 13 文件 |
| scoped 重复公共类 | 9 处定义 / 7 文件 |
| 硬编码中文（非注释） | 6 处脚本 + 4 处模板 |
| 页面骨架达标 | 14 / 47 |
| `useTablePage` 复用 | 14 视图（另 22 视图手写分页） |
| i18n 键 | 977 / 977（同步） |
| JAVA 测试 | 19 类 / 49 用例 |
| 显式 MVP/存根标记 | 9 处（TransferStep、decryptPassword、JTOpen listMembers/readMember、MockAS400Client×2、DataInitializer×3 等） |

---

## 12. 整改优先级建议

### 12.1 P0 — 安全与生产阻断（本周）

1. **修 SQL 注入**：`JobService` 3 处改用 `sq()`/参数占位符 + 白名单校验（B-02）；`SqlQueryService`/`JobScheduleService` 加固只读校验（禁止 `;`、`FOR UPDATE`、`ALTER/...`），加查询超时（B-16/B-17）。
2. **CL 执行白名单**：`CompileService`/`CommandScriptService` 校验命令前缀白名单（如 `CRTBNDRPG/CRTCLPGM/...`），`CompileController` 补 `@PreAuthorize`（B-03/B-04）。
3. **密码加密**：`AS400ClientProviderImpl` 实现 AES-256 加解密（配置 `RXAS400_ENCRYPT_KEY`），迁移历史明文数据（B-01）。
4. **WebSocket 鉴权**：STOMP `CONNECT` 校验 JWT + `/topic/**` 按订阅者权限过滤（部署 topic 校验申请人/审批人、monitor topic 校验 MONITOR_VIEW、通知 topic 改为个人私频道）（S-01）。
5. **CORS 收紧**：按环境配置 allowedOrigins 白名单（S-02）。
6. **修前端两处 Critical**：blob 下载改用 `useStorage.get()` 取解码后 token（F-01）；Monitor 页 instance 全部改为当前服务器 store（F-02）。

### 12.2 P1 — 生产正确性（两周内）

7. **部署状态机加固**：`forServer()` 移入 try 内、失败时显式置 FAILED + 记录真实原因、回滚结果按 `RollbackStep` 返回值判定（B-06/B-07）；加 `@Version` 乐观锁 + 状态前置检查防双跑（B-08）。
8. **告警引擎补语义**：duration 持续判定（连续 N 次/持续秒数）、去重（同规则 OPEN 不重复）、恢复（CLOSED）与恢复通知（B-11/B-12）。
9. **通知投递改单次**：全站通知改"一次 INSERT 批量 + 一次 WS 广播列表"或改个人频道（B-13）。
10. **JT400 客户端补全 + 连接池**：实现 `listMembers/readMember`、库枚举真实化、连接复用（简易池/每个 serverId 单例长连接 + 重连）（B-14/B-15）。
11. **分布式锁**：基于 MySQL `GET_LOCK` 或 Redis 实现 `CollectorScheduler`/Quartz 集群单跑（B-10）。
12. **SourceController 补权限**（B-18）；**ObjectService/Execution 改 DB 分页**（B-19）。
13. **路由重构**：用 `collectMenuPaths(router.getRoutes())` 派生合法路径，删除 MENU_PATHS 硬编码清单（F-03）。

### 12.3 P2 — 质量债（一月内）

14. 前端类型化：为 44 个 api 模块补返回类型，`useTablePage<T>` 泛型化，开启 `noUnusedLocals`（F-04）。
15. 样式中台：`width:100%`→`.w-full`（F-10）；19 处 `flex:1` 占位→`.flex-1`；删除 7 处 scoped 重复类；补齐 `.ml4`/`.mx8` 工具类；`--bg-color` 修引用（§7.2-7.5）。
16. 硬编码收敛：40 处 hex 色换主题变量（重点 Source.vue 代码块、PasswordStrength、Dashboard、As400ServerSelector、calendar、topology）；6+4 处中文/模板文案入 i18n；~12 视图英文标签走 i18n（§8）。
17. 超大组件拆分（Users/menus/layout）与 STOMP 客户端复用（F-06/F-09）；分页组件化收敛到 useTablePage（F-07）。
18. 清理污染测试文件（JwtUtilTest/PipelineExecutorTest）并补充：SQL 注入负面用例、Quartz 作业测试、compile/source 测试（§10）。
19. 错误码体系按域扩容（monitor/system/report/compile/source 独立段），`new BusinessException(ErrorCode.XXX)` 全量到位（§4.3）。
20. 部署默认 `requireApproval=true`、`spring.profiles.active` 取消默认、JWT 密钥启动时强制校验强度（application.yml 硬编码收敛）。

---

*本报告基于当前源码静态审查，未执行端到端运行验证。所有缺陷均标注 `file:line` 便于追溯；修复顺序建议与项目里程碑（MVP→生产）对齐。*