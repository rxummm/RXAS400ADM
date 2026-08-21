# DB2 for i 部署与平台库迁移可行性调研（2026-08-17）

> 依据代码实测（grep 全部 MySQL 特性依赖）+ 外部资料核实（Flyway/DB2 for i 支持现状、jt400 JDBC 兼容性、DB2 for i 分页能力）整理。**结论：平台库迁 DB2 for i 可行、改动可控；数据初始化不依赖 Flyway，手工执行建表 SQL 即可，迁移不作为难点。** 本文档为调研结论，不构成已实施的代码变更。

## 背景与目标（已与客户确认）

- **应用最终部署在 IBM i 本机运行，生产无 MySQL**；dev 环境仍是 Windows + MySQL。
- **平台自身约 47 张表（user/role/permission/metric/alert/audit/config/ibmi_system/...）必须落 DB2 for i**：在 IBM i 上建一个专门数据库（schema/库，如 `RXAS400`）存放平台表。
- **数据初始化不依赖 Flyway**：Flyway 官方不支持 DB2 for i（核实见 §三）。上线后提供全量 DB2 for i 建表 DDL，手工执行初始化即可（`STRSQL` / `RunSQLScript` / jt400 执行均行）；也可选用自研轻量迁移 runner 或原生 SQL 脚本 + 启动校验。
- 需要回答的两个问题：
  1. 平台持久层从 MySQL 迁到 DB2 for i 的改动量；
  2. AS400 系统查询（QSYS2）是否 / 如何用 MyBatis XML 化。

## 一、现有架构核实（多服务器路由模型确认）

代码实测，现有数据访问只有两条路径，**不存在"一个请求同时路由到多个服务器"的场景**：

1. **请求级单服务器**（绝大多数查询页）：前端每次请求带 `X-AS400-Server: <id>` 头 → `As400ServerIdInterceptor` → `As400ServerContextHolder`（ThreadLocal）→ `AS400ClientProvider.current()`。用户登录后选择服务器，之后大部分功能只查询该服务器。典型：`JobService` / `ObjectService` / `PfService` / `IfsService` / `SqlQueryService` / `TopologyService` 等全部走 `current()`。
2. **后台定时任务级多服务器**（监控采集，非请求上下文）：`CollectorScheduler` + `CpuCollector` / `MemoryCollector` / `DiskCollector` / `MsgwCollector` / `LckwCollector` / `NetworkCollector` / `PrinterCollector` 逐个 `clientProvider.forServer(instanceId)` 采集，写入平台库 `rx_metric`。跨服务器对比页 `MonitorController.compare(ids)` 也是**后端按 id 循环**调用 `metricService.overview(id)`（历史指标读平台库 + 每台一个实时 jobs 查询）。

**结论：客户的多服务器模型成立。** 跨服务器比对/聚合页面本来就是"前端分别向多个服务器发请求"或"后端按 id 循环取数"，随后在页面呈现。因此**平台库迁 DB2 for i 不需要引入任何请求级路由数据源**（见 §六）。

## 二、目标架构（IBM i 本机 + 单库 DB2 for i）

```
┌─────────────────────────── IBM i 本机 ───────────────────────────┐
│  Spring Boot (Java 17, 5733OPS OpenJDK)                          │
│    │ MyBatis 主数据源 → 本地 DB2 for i 库 RXAS400                │
│    │    ├── 平台表 47 张（user/role/metric/alert/audit/...）     │
│    │    └── 本分区 QSYS2 系统视图（同库直接查）                   │
│    │ jt400 AS400Client（现有 AS400ClientProvider.forServer）     │
│    │    └── 远程分区 QSYS2 视图 + 命令执行（不变）                │
└──────────────────────────────────────────────────────────────────┘
```

- **MyBatis 主数据源 = 单一数据源（本地 DB2 for i），无任何路由**。平台表与本地分区的 QSYS2 视图在同一个 DB2 for i 实例（一个分区一个关系数据库，库 = schema）。
- 远程分区（多服务器监控、命令执行）继续走现有 `AS400ClientProvider` 的 jt400 per-server 模式，**一行架构代码都不用改**。
- 监控定时采集写 `rx_metric` 等平台表：目标库变为本地 DB2 for i，采集逻辑不变。
- 分布式锁：单实例部署直接关掉 `scheduler-lock`；多实例再议表锁方案（见 §四 GET_LOCK）。

## 三、Flyway 与数据初始化（结论：不构成障碍）

**已核实：Flyway 官方不支持 DB2 for i。** Redgate 支持列表只有 `IBM Db2 for Z` 与 `IBM DB2 LUW`；GitHub `flyway/flyway#105` 明确标 won't fix；Stack Overflow 答复 "currently you cannot use flyway with i-series"。现有 V1~V42 迁移文件全部为 MySQL 方言，无法直接在 IBM i 上跑。

对策（客户已确认，按优先级）：

1. **首选：生成全量 DB2 for i 建表 DDL，上线后手工初始化。** 一次性脚本（建库 + 47 张表 + 种子数据如 admin/权限），执行方式不限（`STRSQL` / `RunSQLScript` / jt400 连接执行）。
2. 可选：自研轻量迁移 runner（`db2i/V{n}__*.sql` + 启动时 jt400 执行 + 自维护版本表，抄 Flyway 概念不用 Flyway）。
3. 可选：生产走 IBM i 原生 SQL/DDL 脚本（SQLPKG/STRSQL）+ 应用启动校验表结构。

**代价：dev 用 MySQL、生产用 DB2 for i，同一 schema 需要维护两套 DDL（MySQL 迁移文件 + DB2 for i 建表脚本）。** 这是迁移的主要维护成本，可接受。

## 四、MySQL → DB2 for i 方言差异与改造点（grep 实测）

| 现状（MySQL） | DB2 for i 对应 | 波及面 |
| --- | --- | --- |
| `AUTO_INCREMENT` 自增主键 | `GENERATED ALWAYS AS IDENTITY` + MyBatis-Plus `IdType` 映射 | **全部 47 表** |
| `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | 删除（CCSID 概念，库级指定） | 全部 47 表 |
| `TINYINT(1)` 布尔 | `SMALLINT` + 实体 `Boolean` 映射 | 多个表（enabled/status 等） |
| 列/表内联 `COMMENT 'xxx'` | `COMMENT ON` / `LABEL ON`（不支持内联） | 多个表 |
| `.last("LIMIT n")` 手写分页 | `FETCH FIRST n ROWS ONLY` | **约 15 处**：`RegionService` / `InspectionService` / `AlertEventService` / `MetricService` / `BaselineService` / `ReportService` / `ReportScheduleService` / `ExecutionService` / `JobScheduleService` / `SqlQueryService` / `AS400ClientProviderImpl` 等 |
| `GET_LOCK` / `RELEASE_LOCK`（`CollectorScheduler` 分布式锁） | DB2 for i 无此函数 → 单节点禁用 `scheduler-lock`，或多实例用表级原子锁 | `monitor.scheduler-lock` 特性 |
| `DATETIME` / `TIMESTAMP` 语义差异 | DB2 for i 用 `TIMESTAMP`，MP 类型映射核对 | 全表时间字段 |
| 反引号/保留字 | 规避（如 `name`/`status` 等尽量不裸用） | 建表脚本统一处理 |
| 表/列名大小写 | IBM i 默认大写，`@TableName`/`@TableField` 按实际命名核对 | 全量实体核对 |
| 中文（UTF-8） | CCSID（建议 1208）真机验证 | 审计日志、消息、文档类中文 |

**关键澄清（分页）**：DB2 for i 从 7.1 TR11 / 7.2 TR3 起原生支持 `LIMIT`/`OFFSET`，MyBatis-Plus `DbType.DB2` 分页插件生成的正是 `OFFSET n ROWS FETCH NEXT m ROWS ONLY`。现有 `.last("LIMIT n")` 在 7.2+ 上大概率也能直接跑，但建议统一改写为 `FETCH FIRST n ROWS ONLY` 以兼容更稳。

## 五、MyBatis-Plus 兼容性

- 分页：`DbType.DB2` 方言可用（见 §四）。需真机验证 `Page` 在 DB2 for i 上的完整 SQL。
- 需真机逐项验证：`IdType`（AUTO vs INPUT）、布尔映射、`LocalDateTime`、Lambda 条件（表列大小写）、`@TableName` 是否显式带库名（`RXAS400.xxx`）。
- Mapper 包名约束：`@MapperScan("com.rxas400adm.**.mapper")` 只扫 `.mapper` 结尾包。**若未来把 QSYS2 查询也放进 MyBatis XML（第二数据源），其 mapper 包不能以 `.mapper` 结尾**，否则被 MySQL 主数据源误扫。

## 六、AS400 查询是否要 MyBatis XML 化（结论：非必须，建议缓做）

- **可行性无问题**：jt400 `AS400JDBCDriver` 是标准 JDBC 3.0/4.0，MyBatis 只认 `DataSource`；QSYS2 表函数在 `<select>` 中就是普通 SQL，`#{}` 绑参、`<if>` 动态条件、`@Select` 注解全支持，社区有大量 jt400 + MyBatis 实战。
- **但本架构下收益有限**，且之前担心的"每分区一个 SqlSessionFactory / AbstractRoutingDataSource"是**多虑**：
  - 请求级单服务器 + 后台 `forServer(id)` 循环已覆盖所有场景，`AS400ClientProvider` 本来就按服务器出客户端，不需要为 MyBatis 引入新路由机制。
  - 本分区 QSYS2 查询若想用 MyBatis，与平台表同属本地 DB2 for i，**一个 SqlSessionFactory 即可**；远程分区的 QSYS2 继续走 jt400 AS400Client。
- **建议**：不在本次迁移立项内做。若后续想统一持久层（把本分区 QSYS2 查询与平台表用同一套 MyBatis），顺带迁移即可，独立做是纯重构零收益。

### 最优方式总结（2026-08-17 追问澄清）

把"能否 XML 化"按查询类型拆开，四类各自的最优处置：

| 查询类型 | 典型 | 最优处置 |
| --- | --- | --- |
| 平台表（rx_*，47 张） | 全部 CRUD/分页 | MyBatis-Plus 主数据源，已 ORM，无需 XML |
| QSYS2 业务型固定列查询 | `JobService.activeJobs()` / `ObjectService` 等 | SQL 不变；想统一可挪 XML 用 `resultType="VO"` 自动映射（省手工 `map.get`），顺带收益，非必须 |
| QSYS2 动态列通用查询 | `SqlQueryService`（任意 SELECT）/ `BusinessService`（任意库.表浏览） | **必须返回 Map，XML 零收益**，保持现 `AS400Client.queryList` |
| QSYS2 per-server 路由 | 走 `clientProvider.current()` / `forServer(id)` 的查询 | **继续 jt400**：MyBatis 主数据源是单一固定连接，运行时换目标服务器需额外路由设施，不值得 |

三个易混概念澄清：

- **"QSYS2 在 XML 里就是普通表"**：MyBatis 不解析 FROM 对象，`QSYS2.SYSTEM_STATUS_INFO` 视图、`TABLE(QSYS2.ACTIVE_JOB_INFO())` 表函数在 `<select>` 里都只是发给 `AS400JDBCDriver` 的普通 SQL 文本，`#{}` 绑参 / `<if>` 动态条件全支持——不存在需要特判的系统对象。
- **per-server 灵活性 ≠ SQL**：灵活的是"目标连接"按请求 `X-AS400-Server` 头运行时解析（`AS400ClientProviderImpl.current()` → ThreadLocal → `forServer(id)` 缓存客户端），SQL 本身两种写法完全一样。XML 化进主数据源后目标变固定单连接，远程分区即够不着。
- **固定列模型 ≠ "SELECT * 再挑列"**：SQL 列清单两法一致（都只查业务需要的列）；差异仅是行转 Java 对象的方式——现实现是 `Map<String,Object>` 大写列名 key（列运行时才知），XML `resultType="VO"` 要求列编译期固定、自动驼峰映射。固定列映射对业务查询是**优点**，只对"列不可预知"的通用查询构成约束（那部分只能退回 Map）。

**为什么"顺带"而非"现在"**：XML 化必须把查询绑到**固定 DataSource** 的 SqlSessionFactory；而现在 QSYS2 查询的目标服务器是运行时按 `X-AS400-Server` 头解析的（jt400 per-server）。现在做就得给 MyBatis 引入 per-server 路由 / 每服务器 SqlSessionFactory——正是上文判定"多虑"的复杂度，映射收益（省 `map.get`）不足以支撑。迁移后本分区 QSYS2 与平台表同落本地 DB2 for i，单 SqlSessionFactory 天然成立，"顺带"才零额外成本；远程分区仍 jt400。

**结论**：QSYS2 XML 化不做独立改造；仅在平台表迁 DB2 for i 时顺带（本分区 QSYS2 与平台表同库，一个 SqlSessionFactory 即可）。通用查询工具保持 Map、远程分区保持 jt400，是最优取舍。

## 七、Java 运行环境

- IBM i 需 `5733OPS` OpenJDK 17（7.4 / 7.5 支持）。Spring Boot 3.3 嵌入 Tomcat 在 IBM i 上运行属常见做法，但需真机验证：字符集 / locale / 时区 / IFS 文件系统 / 启动内存。
- 数据源连接串：`jdbc:as400://localhost/RXAS400;naming=system;errors=full`（jt400 已在项目依赖中）。

## 八、推荐实施路径

1. **POC（不改现有代码）**：IBM i 真机建库 `RXAS400`，用 jt400 验证四项——
   - Spring Boot 3.3 + Java 17 在 IBM i 启动、连接本地 DB2 for i；
   - MyBatis-Plus `DbType.DB2` 分页 SQL 可用；
   - 中文数据读写（CCSID 1208）无乱码；
   - 全量 DB2 for i 建表 DDL 手工执行成功（含种子数据 admin/角色/权限）。
2. **POC 通过 → 迁移立项**：生成 DB2 for i 全量建表脚本；方言改造（IDENTITY / SMALLINT 布尔 / TIMESTAMP / `FETCH FIRST`）；GET_LOCK 处理（单节点关锁）；`application-prod.yml` 数据源切到 `jdbc:as400://...`；dev 保留 MySQL 双方言。
3. **验证清单**：登录 / 动态菜单 / 发布流水线（如已恢复）/ 监控采集与告警 / 审计日志 / 多服务器路由（`X-AS400-Server` 头）/ 服务器对比页 / 巡检 / 容量规划。

## 九、风险清单

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| Flyway 不支持 DB2 for i | 无法用现有迁移体系 | 手工初始化全量建表脚本（客户已接受） |
| 中文乱码 | 审计/消息/文档乱码 | 真机验证 CCSID 1208 |
| 表名大小写 / 保留字 | 查询报错 | 建表脚本与 `@TableName` 统一核对 |
| `.last("LIMIT n")` | 兼容性 | 统一改写 `FETCH FIRST n ROWS ONLY` |
| `GET_LOCK` 无对应 | 多实例重复采集 | 单节点部署关锁；多实例再议表锁 |
| Java 17 on IBM i | 启动/性能异常 | 5733OPS + POC 真机验证 |
| dev/prod 双方言 | 建表脚本维护成本翻倍 | 接受；用脚本生成器减少手工差 |
| 监控采集/告警依赖 `rx_metric` | 指标查询全量受影响 | 迁移立项内优先验证采集闭环 |
