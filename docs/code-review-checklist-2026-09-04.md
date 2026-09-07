# Code Review Checklist — RXAS400ADM

> 基于三轮全量 Code Review（2026-09-04）提炼的检查清单。
> 后续每次 Review 按此清单逐项扫描，避免遗漏。

---

## 1. 后端 — Java 静态安全

### 1.1 空安全与类型转换

| 检查项 | 说明 | 典型反例 |
|--------|------|----------|
| **Integer→int 自动拆箱** | 所有返回 `Integer`/`Long` 的工具方法，调用方必须 null 守卫或用 `*Val()` 重载 | `BpcsRowUtil.intOrNull()` → `int` 赋值导致 NPE |
| **Collectors.toMap 无 merge** | 按非主键字段 collect 时必须提供 merge function `(v1,v2)->v1` | `MenuTreeSupport` 按 id 收集，重复 key 崩溃 |
| **selectById 结果 null 检查** | `mapper.selectById()` 后必须 null 检查或用 `EntityUtil.require()` | 多处 delete 方法静默忽略不存在的 ID |
| **Optional.get() 无 isPresent** | 禁止裸 `.get()`，必须 `.orElse()`/`.orElseThrow()`/`.ifPresent()` | — |

### 1.2 SQL 注入

| 检查项 | 说明 |
|--------|------|
| **MyBatis XML `${}`** | 全局搜索 `${`，禁止 string interpolation；只能用 `#{}` |
| **Native SQL 拼接** | 所有 `FROM` 子句中的表名/库名必须经过 `requireIdentifier()` 白名单校验（`^[A-Z0-9_$#@]+$`） |
| **`@Query(nativeQuery=true)`** | 检查参数是否全部用 `?` 绑定，禁止字符串拼接 |

### 1.3 CL 命令注入

| 检查项 | 说明 |
|--------|------|
| **CL 参数拼接** | 所有 CL 命令参数必须经过 `escapeClString()`（引号转义）或 `requireValidIdentifier()`（白名单） |
| **SPCAUT/GRPPRF/INLMNU** | 这些 CL 关键字参数不能直接拼接用户输入，必须白名单校验 |
| **DangerousClCommandValidator** | 所有 CL 执行路径（含用户 Profile 管理）必须经过校验器 |

### 1.4 异常处理

| 检查项 | 说明 |
|--------|------|
| **RuntimeException 禁止** | 必须用 `BusinessException(ErrorCode.XXX)` 替代 `RuntimeException`/`IllegalArgumentException`/`IllegalStateException` |
| **BusinessException 必须带 ErrorCode** | 禁止 `new BusinessException("msg")`（默认 code=500），必须 `new BusinessException(ErrorCode.XXX, "msg")` |
| **空 catch 禁止** | `catch` 块必须至少 `log.warn/error`，关键路径必须 re-throw 或转换为 `BusinessException` |
| **Controller 层禁止抛业务异常** | 业务逻辑校验必须下沉到 Service 层，Controller 只做参数接收和返回值包装 |
| **@Transactional 零容忍** | 项目已取消全部 `@Transactional`，任何新增即失败 |

### 1.5 并发与幂等

| 检查项 | 说明 |
|--------|------|
| **分布式锁失败处理** | 锁获取失败不能静默降级为单节点模式，必须重试或抛异常 |
| **Token 吊销失败** | 黑名单写入失败不能静默忽略，必须告知调用方 |
| **批量写操作部分失败** | 无事务保证下，多步写操作要考虑中间失败的清理策略 |

---

## 2. 前端 — API 契约一致性

### 2.1 返回类型匹配

| 检查项 | 说明 | 典型反例 |
|--------|------|----------|
| **PageResult vs Array** | 后端返回 `List<VO>` 时前端不能声明为 `{ records: T[]; total: number }` | `listOrderTemplates`/`listRma` |
| **VO vs 基础类型** | 后端返回 `ApiResponse<VO>` 时前端不能声明为 `Promise<string>` | `useOrderTemplate` |
| **CommandResult 不能丢弃** | 后端返回 `CommandResult`（含 success/output）时前端不能声明 `Promise<void>` | `job.ts` 6 个端点 |

### 2.2 接口字段同步

| 检查项 | 说明 |
|--------|------|
| **前端 Interface 与后端 VO 逐字段对比** | 每个 API 的返回类型 Interface 必须与后端 VO record 的字段名、类型一一对应 |
| **禁止重复同名 Interface** | 同文件内不能有同名不同 shape 的 interface（TS 使用最后一个声明） |
| **删除端点泛型** | `request.delete()` 必须显式指定 `<void>` 泛型 |

### 2.3 HTTP 方法与路径

| 检查项 | 说明 |
|--------|------|
| **GET/POST/PUT/DELETE 匹配** | 前端 API 函数的 HTTP 方法必须与后端 `@GetMapping`/`@PostMapping`/... 一致 |
| **路径完全匹配** | 包括路径参数 `{id}` 的位置和命名 |

---

## 3. 前端 — Vue 组件规范

### 3.1 模板安全

| 检查项 | 说明 |
|--------|------|
| **el-table `#default` 禁止窄类型** | 不能 `<template #default="{ row: SomeType }">`，必须裸解构 `{ row }`，调用点断言 |
| **硬编码中文禁止** | 模板中所有可见文本必须用 `$t()` i18n（审计日志匹配常量除外） |
| **`any` 类型最小化** | 业务逻辑中禁止 `any`；`catch (e: any)` 至少用可选链 `e?.response?.data?.message` |

### 3.2 异步安全

| 检查项 | 说明 |
|--------|------|
| **onMounted async** | `onMounted` 中的 async 回调必须 try/catch，否则网络异常导致后续逻辑全部跳过 |
| **v-loading 管理** | loading ref 必须在 `finally` 块中设为 `false`，防止加载状态卡死 |
| **dialog 重置** | 弹窗关闭时必须重置表单数据（`@closed`/`watch(visible)`/打开前重置） |

### 3.3 样式作用域

| 检查项 | 说明 |
|--------|------|
| **父 scoped 到不了子组件内部** | 父组件 `<style scoped>` 只对子组件根元素生效，子组件内部样式必须放全局块或子组件自己的 style |
| **禁止自造类名** | 通用样式必须复用 `common.css` 中的 `.w-full`/`.mr4`/`.section`/`.hint` 等 |
| **颜色用 CSS 变量** | 禁止硬编码颜色值（如 `#909999`），必须用 `var(--el-color-xxx)` |

---

## 4. 数据库迁移

### 4.1 DDL 规范

| 检查项 | 说明 |
|--------|------|
| **CREATE TABLE IF NOT EXISTS** | 所有建表语句必须带 `IF NOT EXISTS` |
| **ALTER TABLE 幂等** | MySQL 8 不支持 `ADD COLUMN IF NOT EXISTS`，需用 `information_schema.COLUMNS` 检查后执行 |
| **CREATE INDEX 幂等** | 用 `information_schema.STATISTICS` 检查后执行 |
| **INSERT 幂等** | 种子数据必须用 `INSERT IGNORE` 或 `WHERE NOT EXISTS` |

### 4.2 跨迁移一致性

| 检查项 | 说明 | 典型反例 |
|--------|------|----------|
| **菜单 parent_id 引用** | 子迁移引用父菜单时，`menu_name`/`title`/`path` 必须与创建迁移完全一致 | V70/V79 引用 `'AS400管理'` 但 V66 创建的是 `'AS400 运维'` |
| **菜单类型** | parent_id 必须指向 `menu_type=1`（目录），不能指向 `menu_type=2`（叶子） | V80 用叶子菜单做父级 |
| **Entity 字段与 DB 列对齐** | 新增 DB 列后 Entity 必须同步声明对应 Java 字段 | `IbmiSystem` 缺 `updatedTime` |

### 4.3 孤立对象

| 检查项 | 说明 |
|--------|------|
| **孤儿表** | DB 中存在但无 Entity/Mapper/Service 引用的表，应清理或归档 |
| **重复 Entity** | 同一张表不应有两个 Entity 类映射（如 `ScheduleAlertEvent`/`AlertEvent`） |

---

## 5. 安全纵深

### 5.1 认证与授权

| 检查项 | 说明 |
|--------|------|
| **每个端点必须有 `@PreAuthorize`** | 新增端点遗漏权限注解即为安全漏洞 |
| **权限码命名** | 格式 `{MODULE}_{ACTION}`，如 `BPCS_VIEW`、`USER_MANAGE` |
| **change-password 必须认证** | 已登录用户才能改密码（`@PreAuthorize("isAuthenticated()")`） |

### 5.2 限流与防护

| 检查项 | 说明 |
|--------|------|
| **登录端点限流** | 所有登录端点（含 AS400 登录）必须归入 `login` 桶 |
| **CSP frame-ancestors** | 必须设置 `frame-ancestors 'self'` 防止点击劫持 |
| **IP 规则正则** | 通配符匹配必须用 `Pattern.quote()` 防止正则注入 |

### 5.3 密码策略

| 检查项 | 说明 |
|--------|------|
| **统一 PasswordPolicy** | 改密/建户/重置密码共用同一套校验（≥8位 + 大写 + 小写 + 数字） |
| **CL 命令中的密码** | 必须经过 `escapeClString()` 转义 |

---

## 6. Review 执行流程

```
每次 Code Review 按以下顺序执行：

1. 后端静态安全（1.1~1.5）     → grep/rg 扫描
2. 前端 API 契约（2.1~2.3）    → 逐文件对比 Controller 返回类型 vs 前端 Interface
3. 前端 Vue 组件（3.1~3.3）    → grep 扫描 + 手工检查
4. 数据库迁移（4.1~4.3）       → 逐迁移文件交叉比对
5. 安全纵深（5.1~5.3）         → grep 扫描
6. 编译验证                     → mvn compile + npm run build
```

---

## 7. 自动化门禁（建议新增）

| 门禁 | 工具 | 说明 |
|------|------|------|
| **OpenAPI spec 生成** | `springdoc-openapi` 已有 | 后端启动后自动产出 `v3/api-docs` |
| **前端类型同步** | `openapi-typescript` | 从 spec 自动生成 TS Interface，消除 F-1~F-8 |
| **CL 命令注入扫描** | 自定义脚本 | 搜索 `append(` + `get` 模式，检查是否经过 `requireIdentifier` |
| **intOrNull 拆箱扫描** | `rg "intOrNull.*=.*int "` | 搜索赋值给原始类型的调用 |
| **迁移幂等扫描** | 已有 `check-migrations.mjs` | 扩展检查 `CREATE TABLE`/`INSERT` 是否带幂等保护 |

---

## 8. 第三轮 Review 修复总结（2026-09-04）

### 8.1 问题原因分类

| 类别 | 根因 | 修复数量 | 典型案例 |
|------|------|----------|----------|
| **VO 字段类型不安全** | `int` 未做 null 守卫，`BpcsRowUtil.intOrNull()` 返回 `Integer` 自动拆箱 | 13处 | `BpcsBomLineVO`、`BpcsLocationInventoryVO` |
| **Collectors.toMap 无 merge** | 并发/数据重复时 key 冲突崩溃 | 4处 | `MenuTreeSupport`、`MenuService` |
| **CL 命令注入** | 用户输入直接拼接 CL 参数 | 1处 | `UserProfileServiceImpl` |
| **前端 API 返回类型错配** | 手写 Interface 与后端实际返回不一致 | 3处 | `listOrderTemplates`、`listRma` |
| **幽灵列** | Entity 改名后模板未同步 | 1处 | `lastUsedTime` 列 |
| **类型重复** | 同文件同名 Interface 不同 shape | 1处 | `WhStock` |
| **void 类型不安全** | 后端返回 `CommandResult` 但前端声明 `Promise<void>` | 6处 | `job.ts` |
| **日志级别不当** | 权限加载失败用 `warn` 而非 `error` | 1处 | `PermissionService` |
| **锁失败静默降级** | 单节点模式锁异常返回 `true` 导致跳过采集 | 1处 | `CollectorScheduler` |
| **Token 吊销静默失败** | 黑名单写入失败用 `log.warn` 忽略 | 1处 | `TokenBlacklistService` |
| **迁移跨版本不一致** | 子迁移引用父菜单时 `menu_name` 不匹配 | 3处 | V70/V79/V80 |
| **Entity 字段缺失** | 新增 DB 列后 Entity 未同步 | 2处 | `IbmiSystem.updatedTime`、`ScheduleAlertEvent.upgradeNotified` |
| **Service 返回 null** | 查询无结果时返回 null 导致调用方 NPE | 5处 | `BackupMonitorServiceImpl`、`BpcsCustomerOverviewServiceImpl` |
| **delete 不检查存在性** | `deleteById` 静默忽略不存在的 ID | 5处 | `DictService`、`IbmiSystemService` |
| **RuntimeException** | 未使用 `BusinessException(ErrorCode)` | 4处 | `AesCryptoService`、`WaybillPdfRenderer` |
| **onMounted 无错误处理** | async 回调无 try/catch，网络异常导致后续逻辑跳过 | 7处 | `Monitor.vue`、`Users.vue` |
| **any 类型泛滥** | `catch (e: any)`、`row: any` 等类型不安全 | 15处 | `rcmx/index.vue`、`wabp/index.vue` |
| **BusinessException 无 ErrorCode** | 单参构造默认 code=500，无错误分类 | 7处 | `ReportBuilderService` |
| **Controller 抛业务异常** | 业务校验逻辑在 Controller 层而非 Service 层 | 2处 | `CalendarController`、`PermissionRequestController` |
| **孤立表** | 无 Entity/Mapper/Service 引用的表 | 2处 | `rx_job_history`、`rx_order_copy_log` |

### 8.2 新增开发规范

| 规范 | 来源 | 影响范围 |
|------|------|----------|
| **VO 字段用 `Integer`/`Long`** | N-1 | 所有 Bpcs*VO |
| **`Collectors.toMap` 必须提供 merge** | N-2 | 所有 `toMap` 调用 |
| **CL 参数必须 `requireValidIdentifier()`** | N-4 | 用户 Profile 管理 |
| **前端 API 返回类型必须与后端 JSON 一致** | F-1/F-2 | `bpcs.ts`、`job.ts` |
| **`CommandResult` 不能声明 `void`** | F-8 | 所有 CL 执行端点 |
| **锁失败必须重试或抛异常** | S-9 | `CollectorScheduler` |
| **Token 吊销失败必须抛异常** | S-10 | `TokenBlacklistService` |
| **菜单迁移必须验证 `menu_name` 一致性** | M-1~M-3 | 所有菜单迁移 |
| **Service 查询必须返回非 null** | Q-1 | 所有 Service |
| **delete 前必须检查存在性** | Q-2 | 所有 delete 方法 |
| **禁止 `RuntimeException`** | Q-3 | 所有 Java 代码 |
| **onMounted async 必须 try/catch** | Q-4 | 所有 Vue 组件 |
| **禁止 `any` 类型注解** | Q-5 | 所有 TypeScript/Vue |
| **`BusinessException` 必须带 `ErrorCode`** | Q-6 | 所有 Java 代码 |
| **Controller 禁止抛业务异常** | Q-7 | 所有 Controller |

---

*文档版本：v1.1（2026-09-04，含第三轮 Review 修复总结）*
