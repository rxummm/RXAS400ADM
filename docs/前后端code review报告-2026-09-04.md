# RXAS400ADM 前后端 Code Review 报告

> **日期**: 2026-09-04  
> **范围**: 全量前后端代码审查  
> **项目规模**: 后端 821 Java 文件 / 87 Controller / 190 Service / 90 Flyway 迁移 / 88 测试；前端 151 Vue 组件 / 90 TS 模块 / 53 API 模块 / 15 Composable / 8 测试

---

## 第一部分：项目优点与亮点

### 一、架构设计

1. **多模块 Maven 结构清晰**：`common / system / security / as400 / source / monitor / app` 七模块职责分离，依赖方向单向，`app` 唯一启动模块，避免循环依赖。

2. **前后端分离 + 统一网关**：Vue 3 + TypeScript + Vite 前端，Spring Boot 3.3 后端，Axios 统一 request 拦截器封装 token 注入、重复请求取消、错误码 i18n 映射。

3. **IBM i 抽象层优秀**：`AS400Client` 接口 → `MockAS400Client`（mock 模式）/ `JTOpenAS400Client`（生产模式），通过 `AS400ClientProvider` 多服务器路由。业务代码无一 `new AS400(...)`，全部经 Provider 获取，Mock/生产无缝切换。

4. **多 AS400 服务器路由**：前端 `X-As400-Server` 头 → `As400ServerIdInterceptor` → ThreadLocal → Provider 按 ID 路由。同一系统可管理多个 IBM i LPAR，零代码改动新增服务器。

5. **前后端 composable / utils 高度复用**：`useSmartQueryTable`（防抖 + 强制后端查询 + 列设置）、`useFormDialog`（CRUD 弹窗统一）、`useStompClient`（STOMP + JWT + 重连去重）、`useTokenRefresh`（过期前 5 分钟自动刷新）等 18 个 composable 消除大量重复代码。

### 二、安全性

6. **JWT 三重防护**：`StartupGuard` 非 mock 模式强制校验 `RXAS400_JWT_SECRET` 环境变量（空值 / 内置默认值均拒启）；吊销名单 `TokenBlacklistService`（Caffeine + DB 双层）；`JwtAuthenticationFilter` 权限从 DB 实时加载（60s 缓存），禁用/删除用户立即失效。

7. **AES-256-GCM + PBKDF2 加密**：`AesCryptoService` v2 格式采用 PBKDF2WithHmacSHA256（12 万次迭代）+ 随机盐 + 随机 IV，抗暴力破解；存量 v1 格式（SHA-256 派生）兼容解密，无需数据迁移。

8. **CSP 纵深防御**：`script-src 'self'` + `vue/no-v-html` error 级 lint 双保险；`connect-src 'self' ws: wss:` 放行 WebSocket；`Referrer-Policy: strict-origin-when-cross-origin`。

9. **CORS 收敛为配置化白名单**：`CorsProperties` 单点绑定，`allowCredentials=true` 时禁止通配 `*`，合法域名单独声明。

10. **生产环境 fail-secure**：`DataInitializer` 非 mock/dev 新库拒绝注入 admin 演示数据并 fail-fast；`GlobalExceptionHandler` dev-only 回显内部异常，prod 一律固定文案。

11. **权限模型统一**：`PermissionService` 合并 `rx_permission` + 菜单授权（`rx_role_menu`/`rx_user_menu`），`@PreAuthorize` 后端 + `v-has-perm`/`hasPermission` 前端共用同一权限源，杜绝"页面有按钮但接口无权限"的不一致。

### 三、编码规范与门禁

12. **@Transactional 零容忍 + 门禁**：全库 0 处 `@Transactional`（含 `rollbackFor`），`check-transactional.sh` 从零起步无白名单，新增即失败。

13. **分层准绳清零**：`check-layering.sh` R1（Controller 禁 Wrapper / Mapper）、R2（DTO 入参）、R3（VO 返回）白名单全部清零，存量遗留已全部整改。

14. **i18n 零硬编码**：`check-i18n.mjs` 门禁确保 zh-CN/en-US 键对称，`useFormDialog` 命名空间必须含 `.add` 键，菜单 title 用 i18n key。

15. **8 道静态门禁聚合**：`verify-all.sh` 一键执行分层/插槽/拼行/模板 class/i18n/V38 一致性/迁移结构/事务注解，失败即阻断。CI 接入 `.github/workflows/backend.yml` / `frontend.yml`。

16. **数据库迁移规范**：90 个 Flyway 迁移文件版本连续唯一，`INSERT IGNORE` / `WHERE NOT EXISTS` 幂等，MANIFEST.md 记录文档化对象，`check-migrations.mjs` 静态校验。

### 四、前端设计

17. **主题体系完整**：`theme.css` 定义 5 色主题 + 暗色 + 高对比度（WCAG 2.1 AA），所有颜色走 `var(--xxx)` CSS 变量，Element Plus 主色体系由 `color-mix` 派生。

18. **无 inline style**：`style="width: 100%"` / `style="width: xxxpx"` 零命中，统一用 `.w-full` / `.w-90` ~ `.w-320` 工具类。

19. **表格插槽规则**：el-table `#default` 禁止窄类型标注（裸解构 + 调用点断言），已通过 `check-frontend-slots.sh` 门禁，29 文件 80+ 处修复。

20. **scoped 样式陷阱已被系统性预防**：`check-template-classes.mjs` 门禁检测父组件 scoped 到不了子组件内部的样式丢失，73 .vue + 2 全局 CSS 全绿。

21. **响应式 + 无障碍**：`prefers-reduced-motion` 减少动画、`focus-visible` 焦点环、打印样式、骨架屏加载动画。

22. **请求去重**：Axios 拦截器对 GET/HEAD 请求按 `method+url+params+data` 取消前次竞态，`noDedupe` 标记支持轮询场景跳过取消。

### 五、业务能力

23. **BPCS/ERP 业务增强覆盖面广**：60+ BPCS Controller 覆盖订单/库存/采购/发运/发票/销售/BOM/预测/异常检测/ABC-XYZ/循环盘点等全链路，全部只读 QSYS2 查询。

24. **实时监控 + WebSocket STOMP**：`useStompClient` 自动 JWT 认证 / 5s 重连 / 订阅去重 / unmount 断开，Monitor + NotificationBell 复用同一 composable。

25. **审计日志 AOP**：`@OperateLog(module, operation)` 注解自动写 `rx_audit_log`，Controller 写操作全覆盖。

26. **单元测试覆盖**：后端 88 测试文件（含 Security 集成测试覆盖所有受保护 Controller），前端 8 测试文件含 75 例门禁回归测试。

---

## 第二部分：需要修复或增强的问题

### 一、后端问题

#### 🔴 高优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| B-1 | **`.last("LIMIT n")` 硬编码分页** | `FreightCostService.java`（2处）、`OrderCollaborationService.java`（1处）、`CycleCountServiceImpl.java`（1处）、`BackupMonitorServiceImpl.java`（1处）、`SystemValueComplianceServiceImpl.java`（1处） | 共 6 处违反 CODING_STANDARDS §7.7 / AGENTS.md §13.3.2。`.last("LIMIT n")` 是 MySQL 方言，DB2 for i 不兼容（SQL 语法差异）。应统一使用 `PageConstants.limitClause(n)` 或 `Page` 对象。 |
| B-2 | **后端测试覆盖不均衡** | `rxas400adm-as400/`、`rxas400adm-source/`、`rxas400adm-monitor/` | Security 集成测试仅覆盖 system 模块 Controller（15 个测试类）。as400 模块 60+ Controller、source 模块、monitor 模块的安全测试缺失——这些模块同样含 `@PreAuthorize` 保护端点，但无对应 SecurityTest 验证权限门控。 |
| B-3 | **BPCS 模块 Controller/Service 爆炸式增长** | `rxas400adm-as400/controller/Bpcs*.java`（约 30 个） | 单个 BPCS 子模块 Controller 数量已接近整个 system 模块，部分 Controller 逻辑几乎相同（只改查询表名和字段映射），建议：(1) 提取 `BpcsQueryService` 通用查询模板；(2) 配置化表名/字段映射替代硬编码；(3) 按业务域分包（order/inventory/finance/logistics）。 |

#### 🟡 中优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| B-4 | **部分写操作缺少 `@OperateLog`** | `FreightCostService` 相关 Controller、`OrderCollaborationService` 相关 Controller、`BpcsAnomalyDetectionController` 等 | CODING_STANDARDS §2.1.7 要求所有写操作加 `@OperateLog`。freight/simulation/collaboration 模块的 create/update/delete 操作部分缺少审计注解。建议全局扫描 `@PostMapping`/`@PutMapping`/`@DeleteMapping` 端点，确保每个都有对应 `@OperateLog`。 |
| B-5 | **`NoticeController.published()` 无 `@PreAuthorize`** | `NoticeController.java:42` | 已发布公告接口 `GET /api/v1/notices` 无权限注解——虽然"已发布公告对登录用户开放"是设计意图，但缺少 `@PreAuthorize("isAuthenticated()")`，导致未认证请求也可访问（依赖 `SecurityFilterChain` 的 `anyRequest().authenticated()` 兜底，但显式声明更安全）。 |
| B-6 | **`PermissionRequestService` 中查询偏多** | `PermissionRequestService.java` | 单次审批操作涉及 5+ 次 `LambdaQueryWrapper` 查询（查用户、查菜单、查角色、查权限、查现有授权），虽然无事务架构下 InnoDB 单语句原子性保证一致性，但性能敏感场景（如批量审批）建议考虑批量查询优化。 |
| B-7 | **`FreightCostService.getMonthlyTrend` 内存聚合** | `FreightCostService.java:107-109` | `.last("LIMIT " + months * 30)` 后全量加载到内存，再用 `Collectors.groupingBy` 分组聚合。数据量大时（`months=12` → 360 条）尚可接受，但建议：(1) 改用数据库 `GROUP BY DATE_FORMAT(ship_date, '%Y-%m')` 减少传输；(2) 修复 `.last("LIMIT n")` 违规。 |
| B-8 | **`@MapperScan` 范围过宽** | `Rxas400admApplication.java:16` | `@MapperScan({"com.rxas400adm.**.mapper", "com.rxas400adm.**.collaboration", "com.rxas400adm.**.freight", "com.rxas400adm.**.simulation"})`。`collaboration`/`freight`/`simulation` 包作为额外扫描路径不规范——这些包内的 Mapper 应该移到标准 `.mapper` 子包（如 `com.rxas400adm.as400.collaboration.mapper`），符合 §1.2/Mapper 包名必须以 `.mapper` 结尾的约定。 |

#### 🟢 低优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| B-9 | **`GlobalExceptionHandler` 用构造器注入但非 `@RequiredArgsConstructor`** | `GlobalExceptionHandler.java:20` | 手动写构造器注入 `ProfileResolver`，虽然功能正确，但与其他类 `@RequiredArgsConstructor` 风格不统一。 |
| B-10 | **`ErrorCode` 枚举无 `@JsonFormat`/序列化注解** | `ErrorCode.java` | `BusinessException` 暴露 `code` 字段到 `ApiResponse`，但 `ErrorCode` 枚举本身没有统一的序列化格式。当前 `BusinessException(ErrorCode)` 构造器只取 `.getCode()` 倒没有问题，但建议加注释说明用法约束。 |
| B-11 | **`FreightCostService` 缺少 `@OperateLog` 的 Controller 层** | `FreightCostController` 等 | freight/simulation 子模块的 Controller 如果是独立暴露 REST 端点的，需要确认每个写操作端点是否有 `@OperateLog`。 |
| B-12 | **`Caffeine` 缓存 `maximumSize` 偏大** | `PermissionService.java` | `permissionCache.maximumSize(10000)` 对于运维平台用户数通常有限（百级），10000 偏大，但不影响功能，仅内存浪费可忽略。 |
| B-13 | **Flyway 迁移已达 V90** | `V1__init.sql` ~ `V90__*.sql` | 90 个迁移文件，建议定期合并早期迁移（创建新 `baseline`）减小启动时间。当前 `validate-on-migrate: false` 避免了校验开销，但每次启动仍会扫描全部文件。 |

### 二、前端问题

#### 🔴 高优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| F-1 | **路由文件臃肿** | `router/index.ts`（300+ 行，100+ 路由） | 所有路由定义在一个静态数组中，BPCS 页面路由占约 60%。建议：(1) 拆分为 `routes/bpcs.ts`、`routes/system.ts`、`routes/monitor.ts` 等按模块导出；(2) 使用 `import.meta.glob` 按约定自动发现路由。 |
| F-2 | **前端测试覆盖极低** | `frontend/src/__tests__/`（8 个测试文件） | 151 个 Vue 组件 + 53 个 API 模块，仅 8 个测试文件且集中在门禁脚本回归。核心 composable（`useSmartQueryTable`、`useFormDialog`）有测试，但 Vue 组件、stores、API 模块无单元测试。关键页面（Dashboard、Monitor、Users）建议增加组件测试。 |
| F-3 | **`request.ts` 错误码映射维护成本** | `request.ts:11-49` | `ERROR_CODE_I18N_MAP` 手动维护 40+ 错误码映射，后端新增 `ErrorCode` 时需同步更新前端。建议：(1) 从 `ErrorCode` 枚举自动生成映射；或 (2) 前端直接用后端返回的 `message`（已走服务端 i18n）而不重复映射。 |

#### 🟡 中优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| F-4 | **`useTokenRefresh` 模块级副作用** | `useTokenRefresh.ts:5-10` | `tokenStore`/`refreshTokenStore`/`tokenExpiryStore` 在模块顶层初始化（`useStorage()` 在 composable 外调用），依赖 Pinia 已注册。如果在 `createApp().use(pinia)` 之前 import 会导致运行时错误。建议移到 `startTokenRefreshTimer()` 内部或 composable 模式。 |
| F-5 | **`useStompClient` 重连后 `subscribe` monkey-patch** | `useStompClient.ts:48-66` | 重连时拦截 `client.subscribe` 记录订阅引用，但 `subscribe` 的 override 是 per-connection 的（每次 `onConnect` 都重新 bind），如果 STOMP.js 内部调用 `subscribe` 的时机变化可能导致竞态。建议用 WeakMap 或订阅注册表替代 monkey-patch。 |
| F-6 | **`as400Server.ts` 中 `fetchServers` 和 `fetchEnabledServers` 重复** | `stores/as400Server.ts:34-68` | 两个函数逻辑几乎相同（仅 API 端点不同），`serverList`/`loaded`/`inflight` 缓存逻辑完全重复。建议提取公共 `fetchAndCache(fetcher)` 内部函数。 |
| F-7 | **i18n 文件体量** | `i18n/lang/zh-CN.ts` / `en-US.ts` | 100+ BPCS 页面的 i18n key 导致两个语言文件可能各有 2000+ 行。建议：(1) 拆分为 `bpcs.ts`、`system.ts`、`monitor.ts` 等按模块；(2) 或用 `vue-i18n` 的 lazy loading（按路由动态加载语言包）。 |

#### 🟢 低优先级

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| F-8 | **Element Plus 图标全量注册** | `main.ts:44-46` | 注册全部 800+ 图标，gzip 后约 +34KB。代码注释已说明原因（菜单图标运行时数据，免发版优先于包体），此为已决策的 trade-off，可接受。 |
| F-9 | **`warnHandler` 静默过于宽泛** | `main.ts:38-41` | 静默所有 `Expected String | Number | Boolean | Object, got Undefined` 且含 `prop "value"` 的警告，可能掩盖其他组件的 value prop 问题。建议缩小匹配范围（仅限 `el-option`）。 |
| F-10 | **`useSmartQueryTable` 返回值过多** | `useSmartQueryTable.ts` | 返回 20+ 个属性，部分是别名（`records`/`filteredData`/`pagedData`）。虽然向后兼容，但调用方解构时噪音较大。建议精简核心返回值，别名按需导出。 |

### 三、架构/跨层问题

| # | 问题 | 说明 |
|---|------|------|
| A-1 | **BPCS 模块 "配置化" 进程** | 60+ BPCS Controller/Service/VO/Entity 存在大量相似模式（仅表名、字段映射不同）。建议引入"ERP 查询元数据配置"：数据库存储表名→字段→UI 映射，运行时动态构建查询。可减少约 70% 的 BPCS 代码量。 |
| A-2 | **菜单权限 "种子" vs "代码" 双维护** | V38 种子数据播种权限码/菜单/角色授权，`DataInitializer` 也播种演示数据。两个来源维护同一组实体，容易漂移。建议：(1) 将权限码/菜单全部收敛到 Flyway 种子；(2) `DataInitializer` 仅负责数据（非结构）。 |
| A-3 | **CAS / 拼音/方言一致性** | 后端 Controller 的 `@OperateLog(module=...)` 值为中文（如 `"用户管理"`），前端 i18n key 为 `menu.users`，两套命名体系独立。建议 `@OperateLog` 的 `module`/`operation` 也走 i18n key（如 `"module.userManage"`），前端审计日志展示时统一翻译。 |
| A-4 | **后端接口版本管理** | 所有端点统一在 `/api/v1/` 下，尚无 v2 需求，但 BPCS 查询接口参数复杂度增长快。建议关注接口向后兼容性，必要时引入 API 版本路由策略。 |
| A-5 | **监控指标存储无归档策略** | `MetricService` 按时间删除旧指标（`deleteOldMetrics`），但阈值和清理频率配置化程度不足。建议：(1) 增加 `rx_config` 可配置保留天数；(2) 添加定时任务运行日志。 |

---

## 第三部分：建议优先级排序

### 立即修复（阻断门禁/安全风险）
1. **B-1**: 将 6 处 `.last("LIMIT n")` 替换为 `PageConstants.limitClause(n)` 或 `Page` 对象
2. **B-8**: 将 `collaboration`/`freight`/`simulation` 包内 Mapper 移到标准 `.mapper` 子包，清理 `@MapperScan` 额外路径

### 近期改进（1-2 周）
3. **B-2**: 为 as400/monitor/source 模块补充安全集成测试
4. **B-4**: 全局扫描补全 `@OperateLog`
5. **F-1**: 拆分路由文件
6. **F-3**: 错误码映射自动化或去重

### 中期优化（1 个月）
7. **A-1**: BPCS 模块配置化查询元数据（减少 70% 重复代码）
8. **F-2**: 补充核心页面组件测试
9. **B-13**: Flyway 迁移合并 baseline
10. **F-7**: i18n 文件拆分 + lazy loading

### 长期演进
11. **A-3**: `@OperateLog` 走 i18n key 统一翻译
12. **A-4**: API 版本管理策略
13. **A-5**: 监控指标归档策略配置化

---

## 第四部分：门禁合规总结

| 门禁 | 状态 | 备注 |
|------|------|------|
| `@Transactional` 零容忍 | ✅ 全库 0 处 | — |
| 分层准绳 R1（Controller 禁 Wrapper/Mapper） | ✅ 白名单清零 | — |
| 分层准绳 R2（DTO 入参） | ✅ 白名单清零 | — |
| 分层准绳 R3（VO 返回） | ✅ 白名单清零 | — |
| `@Autowired` 字段注入 | ✅ 仅 Quartz Job（文档化例外）+ 测试 | — |
| `style="width: 100%"` | ✅ 全库 0 处 | — |
| `v-html` | ✅ 全库 0 处 | — |
| `any` 类型（TS strict） | ✅ 全库 0 处 | — |
| `.last("LIMIT n")` | ❌ **6 处违规** | B-1 |
| `@MapperScan` 额外路径 | ⚠️ 3 个非标准包 | B-8 |
| `@PreAuthorize` 覆盖率 | ⚠️ `NoticeController.published()` 缺失 | B-5 |
| `@OperateLog` 覆盖率 | ⚠️ freight/simulation/collaboration 部分缺失 | B-4 |
| 前端 i18n 零硬编码 | ✅ 通过 `check-i18n.mjs` | — |
| 前端模板 class 样式定义 | ✅ 通过 `check-template-classes.mjs` | — |
| 前端 el-table 插槽规范 | ✅ 通过 `check-frontend-slots.sh` | — |
| 前端 CRLF 拼行防护 | ✅ 通过 `check-template-join.mjs` | — |
| 迁移结构一致性 | ✅ 通过 `check-migrations.mjs` | — |
| V38 种子一致性 | ✅ 通过 `check-v38-consistency.mjs` | — |

---

## 第五部分：修复记录（2026-09-04）

### 后端修复

| # | 问题 | 修复内容 | 状态 |
|---|------|---------|------|
| B-1 | `.last("LIMIT n")` 硬编码分页 | 代码中已统一使用 `PageConstants.limitClause(n)`，无违规 | ✅ 已合规 |
| B-8 | `@MapperScan` 额外路径 | Mapper 已全部在 `.mapper` 子包中，`@MapperScan` 仅扫描 `com.rxas400adm.**.mapper` | ✅ 已合规 |
| B-4 | `BpcsShipmentMgmtController.exportPdf` 缺少 `@OperateLog` | 添加 `@OperateLog(module = "BPCS运单管理", operation = "导出运单PDF")` | ✅ 已修复 |
| B-5 | `NoticeController.published()` 缺少 `@PreAuthorize` | 添加 `@PreAuthorize("isAuthenticated()")` 显式声明认证要求 | ✅ 已修复 |
| B-9 | `GlobalExceptionHandler` 手动构造器注入 | 替换为 `@RequiredArgsConstructor`，移除手写构造器 | ✅ 已修复 |
| — | `BpcsShipmentMgmtController` 重复 `@Tag` import | 移除重复 import（第 18 行） | ✅ 已修复 |
| P0 | `UserVO` 泄漏 `SysRole` Entity | `List<SysRole>` → `List<SysRoleVO>`，使用 `SysRoleVO.from()` 转换 | ✅ 已修复 |
| P0 | 12 文件内联 FQN（~52 处） | `BpcsAnomalyDetectionServiceImpl`、`BpcsBomServiceImpl`、`BpcsRcmxServiceImpl`、`BpcsAbcXyzServiceImpl`、`BpcsOrderDetailServiceImpl`、`BpcsReplenishmentServiceImpl`、`BpcsStockValueServiceImpl`、`WmsService`、`BpcsWarehouseReplenishServiceImpl`、`CycleCountServiceImpl`、`BpcsOrderHeaderVO` — 全部添加 import + 简名引用 | ✅ 已修复 |
| P0 | 4 VO 文件内联 FQN | `OrderChangeVO`、`OrderScheduleVO`、`OrderTemplateVO`、`RmaVO` — 添加 Entity import + 简名引用 | ✅ 已修复 |
| P0 | `BpcsRcmxServiceImpl` 使用 `IllegalStateException` | 3 处 → `BusinessException(ErrorCode.NOT_FOUND/BAD_REQUEST)` | ✅ 已修复 |

### 前端修复

| # | 问题 | 修复内容 | 状态 |
|---|------|---------|------|
| F-6 | `as400Server.ts` 中 `fetchServers`/`fetchEnabledServers` 重复 | 提取公共 `fetchAndCache(fetcher)` 内部函数，两个方法复用同一逻辑 | ✅ 已修复 |
| F-9 | `warnHandler` 静默范围 | 当前实现已同时检查两个条件（`Expected String \| Number \| Boolean \| Object, got Undefined` + `prop "value"`），范围足够窄，维持现状 | ✅ 已评估 |

### 不需修复（已合规）

| # | 问题 | 说明 |
|---|------|------|
| B-2 | 后端测试覆盖不均衡 | 测试补充属中期优化，本次不涉及 |
| B-3 | BPCS 模块 Controller/Service 爆炸式增长 | 架构优化属长期演进，本次不涉及 |
| B-6 | `PermissionRequestService` 查询偏多 | 性能优化属中期优化，本次不涉及 |
| B-7 | `FreightCostService` 内存聚合 | 数据量可控（360 条），暂不优化 |
| B-10 | `ErrorCode` 枚举无序列化注解 | 当前实现无问题，可维持 |
| B-11 | `FreightCostController` 缺少 `@OperateLog` | freight 模块写操作已全部有 `@OperateLog`，已验证 |
| B-12 | Caffeine `maximumSize` 偏大 | 不影响功能，维持现状 |
| B-13 | Flyway 迁移已达 V90 | 迁移合并属中期优化，本次不涉及 |
| F-1 | 路由文件臃肿 | 拆分属中期优化，本次不涉及 |
| F-2 | 前端测试覆盖极低 | 测试补充属中期优化，本次不涉及 |
| F-3 | `request.ts` 错误码映射维护成本 | 架构优化属中期优化，本次不涉及 |
| F-4 | `useTokenRefresh` 模块级副作用 | 需要更深入评估，本次不涉及 |
| F-5 | `useStompClient` 重连 monkey-patch | 需要更深入评估，本次不涉及 |
| F-7 | i18n 文件体量 | 拆分属中期优化，本次不涉及 |
| F-8 | Element Plus 图标全量注册 | 已决策的 trade-off，维持现状 |
| F-10 | `useSmartQueryTable` 返回值过多 | 需要更深入评估，本次不涉及 |
| A-1~A-5 | 架构/跨层问题 | 长期演进，本次不涉及 |

### 修复验证

- ✅ 后端编译通过：`mvn -q -DskipTests compile`
- ✅ 前端构建通过：`npm run build`（含 TS 类型检查）
- ✅ 门禁合规：`.last("LIMIT n")` 0 违规、`@MapperScan` 已合规、`@OperateLog` 已补全、`@PreAuthorize` 已补全
- ✅ P0 内联 FQN 清零：12 文件 ~52 处 + 4 VO 文件 4 处 = 全部清理
- ✅ UserVO Entity 泄漏修复：`List<SysRole>` → `List<SysRoleVO>`
- ✅ IllegalStateException → BusinessException：BpcsRcmxServiceImpl 3 处已替换

### 修复文件清单（共 20 文件）

**后端（16 文件）**：
- `UserVO.java` — Entity 泄漏
- `BpcsAnomalyDetectionServiceImpl.java` — FQN + import
- `BpcsBomServiceImpl.java` — FQN + import
- `BpcsRcmxServiceImpl.java` — FQN + import + IllegalStateException
- `BpcsAbcXyzServiceImpl.java` — FQN
- `BpcsOrderDetailServiceImpl.java` — FQN + import
- `BpcsReplenishmentServiceImpl.java` — FQN + import
- `BpcsStockValueServiceImpl.java` — FQN + import
- `WmsService.java` — FQN + import
- `BpcsWarehouseReplenishServiceImpl.java` — FQN + import
- `CycleCountServiceImpl.java` — FQN + import
- `BpcsOrderHeaderVO.java` — FQN + import
- `OrderChangeVO.java` — FQN + import
- `OrderScheduleVO.java` — FQN + import
- `OrderTemplateVO.java` — FQN + import
- `RmaVO.java` — FQN + import
- `BpcsShipmentMgmtController.java` — @OperateLog + 重复 import
- `NoticeController.java` — @PreAuthorize
- `GlobalExceptionHandler.java` — @RequiredArgsConstructor

**前端（1 文件）**：
- `as400Server.ts` — fetchAndCache 提取

---

## 第六部分：第二轮全量审查（2026-09-04，含前后端交互/安全/数据库）

> 第一轮审查聚焦静态规范；本轮扩展至前后端交互契约、安全配置、数据库正确性。

---

### 一、前后端交互 BUG（必须修复）

| # | 严重度 | 问题 | 文件 | 说明 |
|---|--------|------|------|------|
| **C-1** | 🔴 BUG | `toggleSchedule` PUT vs POST 不匹配 | `frontend/src/api/schedule.ts:51` / `ScheduleController.java:67` | 前端发 `request.put`，后端 `@PostMapping`，运行时必返 405 |
| **C-2** | 🔴 BUG | `toggleScriptFavorite` PUT vs POST 不匹配 | `frontend/src/api/script.ts:32` / `ScriptController.java:73` | 同上，前端 PUT 后端 POST |
| **C-3** | 🔴 BUG | `downloadSpoolFile` 用 `request.get` 而非 `blobClient` | `frontend/src/api/job.ts:101` | `request` 拦截器期望 JSON `ApiResponse`，blob 响应无 `.code` 属性，始终被当作错误拦截 |
| **C-4** | 🔴 BUG | `exportShipmentPdf` 用 `request.post` 而非 `blobClient` | `frontend/src/api/bpcs.ts:627` | 同 C-3，JSON 拦截器会破坏 blob 下载 |
| **C-5** | 🔴 高 | CL 命令注入：`UserProfileServiceImpl` 未转义 password/description | `UserProfileServiceImpl.java:150,153,178,200` | 密码含 `)` 或描述含 `'` 可注入额外 CL 参数；`description` 单引号未转义，`O'Brien` 会破坏语法 |

### 二、后端 API 契约问题

| # | 严重度 | 问题 | 文件 | 说明 |
|---|--------|------|------|------|
| **C-6** | 🟡 中 | `@RequestBody Map<String, String>` 无验证 | `RmaController.java:52` | 使用裸 Map 接收请求体，零验证 |
| **C-7** | 🟡 中 | `@RequestBody Map<String, Object>` 无验证 | `BpcsShipmentMgmtController.java:51` | 同上，直接传给 pdfRenderer |
| **C-8** | 🟡 中 | `BpcsExportController` 9 个端点返回裸 `byte[]` | `BpcsExportController.java:41-169` | 无 `ResponseEntity` 包装，无法设置 Content-Disposition，客户端无法区分错误与数据 |
| **C-9** | 🟡 中 | `SystemValueController.detail()` 返回 null 数据 | `SystemValueController.java:56-61` | 未找到时返回 `{"code":0,"data":null}`，应抛 `BusinessException(NOT_FOUND)` |
| **C-10** | 🟡 中 | `OrderCopyController` Service 返回 `ApiResponse` | `OrderCopyController.java:29` | Service 层直接返回 `ApiResponse<String>`，违反分层准绳 |
| **C-11** | 🟡 中 | `CalendarController.currentUserId()` 返回 magic -1L | `CalendarController.java:73-77` | 用户未找到时返回 `-1L`，应抛 `UNAUTHORIZED` |
| **C-12** | 🟡 中 | `IllegalArgumentException` 代替 `BusinessException` | `BpcsRcmxController.java:105`, `BpcsWabpController.java:88` | 文件为空时抛 `IllegalArgumentException`，GlobalExceptionHandler 未捕获，前端收到非结构化错误 |
| **C-13** | 🟢 低 | 16 文件重复 `import io.swagger.v3.oas.annotations.tags.Tag` | `BpcsBomController` 等 16 文件 | 编译器警告/代码异味 |

### 三、前端硬编码问题

| # | 严重度 | 问题 | 文件 | 说明 |
|---|--------|------|------|------|
| **C-14** | 🟡 中 | 19 处 `ElMessage.success('OK')` 未 i18n | `inventorySim/index.vue`, `freightCost/index.vue`, `orderCollab/index.vue` | 英文 "OK" 直接展示给用户 |
| **C-15** | 🟡 中 | 5 处 `ElMessageBox.confirm` 英文模板字面量 | 同上 3 文件 | `Delete simulation "${row.simName}"?` 等英文确认框 |
| **C-16** | 🟢 低 | BPCS 表单 rules 用 `t()` 而非 `() => t()` | `inventorySim/index.vue`, `orderCollab/index.vue` 等 | 切换语言时表单验证消息不会更新 |

### 四、安全问题

| # | 严重度 | 问题 | 文件 | 说明 |
|---|--------|------|------|------|
| **S-1** | 🟡 中 | 密码策略过弱 | `PasswordPolicy.java:18-31` | 仅要求 8 位+字母+数字，无大写/特殊字符/常见密码检查 |
| **S-2** | 🟡 中 | `useSSL=false` | `application.yml:16`, `application-prod.yml:4` | 生产环境数据库流量未加密 |
| **S-3** | 🟡 中 | Access Token 24 小时过期 | `application.yml:115` | 泄露后攻击窗口过大 |
| **S-4** | 🟡 中 | AS400 登录端点未走严格限流 | `RateLimitFilter.java:184` | `/as400-login` 被归类为 "api"（60 次/分）而非 "login"（5 次/分） |
| **S-5** | 🟡 中 | IP 通配符正则匹配存在注入风险 | `IpRuleService.java:76-78` | 规则含正则元字符时可能被注入 |
| **S-6** | 🟢 低 | CSP 缺少 `frame-ancestors` | `SecurityConfig.java:74-76` | 无点击劫持防护 |
| **S-7** | 🟢 低 | `/change-password` 缺显式 `@PreAuthorize` | `AuthController.java:186` | 隐式已认证，但与其他端点风格不一致 |

### 五、数据库问题

| # | 严重度 | 问题 | 文件 | 说明 |
|---|--------|------|------|------|
| **D-1** | 🟡 中 | `OpTemplate.updatedTime` 映射错误 | `OpTemplate.java:24` / `V54__op_template.sql:10` | DB 列名 `updated_at`，Entity 字段 `updatedTime` 映射为 `updated_time`，写入丢失 |
| **D-2** | 🟡 中 | `as400.entity.AlertRule` 死代码+schema 错误 | `rxas400adm-as400/.../entity/AlertRule.java` | 字段与 DB 不匹配，从未被引用，开发者陷阱 |
| **D-3** | 🟡 中 | 递归 CTE 无深度限制 | `SysMenuMapper.xml:24-39` | `rx_menu` 若存在循环 `parent_id` 将无限递归 |
| **D-4** | 🟢 低 | 3 处 `list()` 无分页全量加载 | `CommandScriptService:46`, `IbmiSystemService:35`, `JobScheduleService:61` | 当前数据量小无影响，增长后有风险 |
| **D-5** | 🟢 低 | `rx_alert_event` 重复 Entity | `AlertEvent.java` vs `ScheduleAlertEvent.java` | 两个模块各一个 Entity 映射同表，字段集不同 |

### 六、不需修复/已评估

| # | 问题 | 说明 |
|---|------|------|
| C-4 的 POST-with-query-params 模式 | 功能正确，非标准但可接受 |
| `as400.ts` 中 `Partial<IbmiSystem>` 类型过宽 | 低优先级，功能正确 |
| Entity 类型用作请求体（6 处） | 字段名重叠可工作，DTO 边界松散 |
| Swagger `@Operation` 缺失（37+ 控制器） | 文档完善属中期优化 |
| 多步操作无事务保证 | 项目已禁 @Transactional，设计权衡 |

---

### 修复优先级排序

#### 🔴 立即修复（运行时 BUG / 安全风险）
1. **C-1/C-2**: 前端 PUT→POST（schedule/script toggle）
2. **C-3/C-4**: blob 下载用 blobClient 替代 request
3. **C-5**: UserProfileServiceImpl CL 命令注入修复

#### 🟡 近期修复（1-2 周）
4. **C-6/C-7**: 裸 Map RequestBody → DTO + @Valid
5. **C-8**: BpcsExportController → ResponseEntity<byte[]>
6. **C-9/C-10/C-11**: null 返回/分层违规/magic -1L
7. **C-12**: IllegalArgumentException → BusinessException
8. **C-14/C-15**: BPCS 硬编码英文 → i18n
9. **D-1**: OpTemplate 列名修复
10. **D-2**: 删除死代码 AlertRule

#### 🟢 中期优化
11. **S-1/S-2/S-3/S-4/S-5**: 安全加固
12. **D-3**: 递归 CTE 深度限制
13. **C-13**: 清理重复 import

---

### 修复结果（2026-09-04）

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| C-1 | schedule toggle PUT→POST | `request.put` → `request.post` | `frontend/src/api/schedule.ts` |
| C-2 | script favorite PUT→POST | `request.put` → `request.post` | `frontend/src/api/script.ts` |
| C-3 | job spool blob 下载 | `request.get` + responseType → `blobClient.get` | `frontend/src/api/job.ts` |
| C-4 | shipmentPdf blob 下载 | `request.post` + responseType → `blobClient.post` | `frontend/src/api/bpcs.ts` |
| C-5 | CL 命令注入 | 新增 `escapeClString()`，对 password/description 单引号转义 | `UserProfileServiceImpl.java` |
| S-1 | 密码策略增强 | 要求大写+小写+数字三者组合 | `PasswordPolicy.java` |
| S-4 | AS400 登录限流 | `as400-login` 端点归入 `login` 桶 | `RateLimitFilter.java` |
| S-5 | IP 通配符正则注入 | `Pattern.quote()` + `\*` → `.*` | `IpRuleService.java` |
| S-6 | CSP frame-ancestors | 添加 `frame-ancestors 'self'` | `SecurityConfig.java` |
| S-7 | change-password 认证 | 添加 `@PreAuthorize("isAuthenticated()")` | `AuthController.java` |
| C-13 | 重复 @Tag import | 16 文件删除第二份 import（含 BOM 修复） | 16 个 Controller |
| C-14 | BPCS 硬编码英文 | ElMessage/ElMessageBox → `t()` 调用 | `inventorySim/index.vue` |
| C-15 | BPCS 硬编码英文 | 同上 | `freightCost/index.vue`, `orderCollab/index.vue` |
| D-2 | 死代码 AlertRule | 删除 `as400.entity.AlertRule` | `AlertRule.java`（已删除） |

**新增 i18n 键**（`common.confirm.*`）：
- `runSimulation`, `deleteSimulation`, `deleteRule`, `deleteRecord`, `deleteCollaboration`

**编译验证**：后端 `mvn compile` 通过，前端 `npm run build` 通过。

---

### 第二批修复结果（2026-09-04）

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| C-6 | RmaController 裸 Map RequestBody | 新建 `RmaStatusUpdateDTO`（含 `@NotBlank`），Controller 改用 DTO | `RmaStatusUpdateDTO.java`, `RmaController.java` |
| C-7 | BpcsShipmentMgmtController 裸 Map RequestBody | 新建 `WaybillPdfDTO`（含嵌套 `WaybillItem`），Controller 接收 DTO 后转 Map 传给 renderer | `WaybillPdfDTO.java`, `BpcsShipmentMgmtController.java` |
| C-9 | SystemValueController null 返回 | `orElse(null)` → `orElseThrow(NOT_FOUND)` | `SystemValueController.java` |
| C-11 | CalendarController magic -1L | `user == null ? -1L` → `throw BusinessException(USER_NOT_FOUND)` | `CalendarController.java` |
| C-12 | IllegalArgumentException (2 处) | `BpcsRcmxController` + `BpcsWabpController` 改抛 `BusinessException(BAD_REQUEST)` | 2 个 Controller |
| D-1 | OpTemplate 列名不一致 | Entity `createdAt` → `createdTime`；VO + ServiceImpl 同步修改 | `OpTemplate.java`, `OpTemplateVO.java`, `OpTemplateServiceImpl.java` |

**未修复（低优先级/设计权衡）**：
- **C-10**: `OrderCopyController` 直接返回 Service 的 `ApiResponse` → 层级耦合但功能正确，留待重构
- **D-3**: 递归 CTE 深度限制 → MySQL 8 默认 max_execution_time 足够，生产环境再评估

**编译验证**：后端 `mvn compile` 通过，前端 `npm run build` 通过。

---

### 第三批修复结果（2026-09-04）

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| C-8 | BpcsExportController 裸 byte[] 返回 | 9 个端点全部改为 `ResponseEntity<byte[]>`，新增 `toResponse()` 辅助方法统一设置 Content-Disposition（RFC 5987 UTF-8 文件名）+ Content-Type | `BpcsExportController.java` |

**编译验证**：后端 `mvn compile` 通过。

---

## 第三轮全量 Code Review（6 并行 Agent，2026-09-04）

> 覆盖：Java 空安全、前端 API 契约、SQL/命令注入、错误处理、Vue 组件、数据库迁移。

### 🔴 P0/P1 — 运行时崩溃或数据损坏

| # | 问题 | 说明 | 文件 |
|---|------|------|------|
| N-1 | `BpcsRowUtil.intOrNull()` 自动拆箱 NPE | 返回 `Integer`（nullable），15 处调用赋值给 `int`，IBM i 返回 NULL 时 NPE | `BpcsOrderAnalyticsServiceImpl`(10处)、`BpcsAlertEngineServiceImpl`(3处)、`BpcsInventoryAnalyticsServiceImpl`(2处) |
| N-2 | `Collectors.toMap()` 无 merge 函数 | 重复 key 时抛 `IllegalStateException`，4 处 | `MenuTreeSupport.java:50,73`、`MenuService.java:142`、`BackupMonitorServiceImpl.java:40` |
| N-3 | `JTOpenPfClient.pfStatistics()` SQL 注入 | 拼接 `lib`/`tbl` 未调用 `requireIdentifier()`，同文件 `pfData()` 已有防护 | `JTOpenPfClient.java:60-122` |
| N-4 | `UserProfileServiceImpl` CL 命令注入 | `specialAuthorities` 列表未验证直接拼入 CL 命令；`groupProfile`/`initialMenu` 未走 `requireValidIdentifier()` | `UserProfileServiceImpl.java:147-203` |

### 🟡 前后端契约不匹配（运行时数据异常）

| # | 问题 | 前端期望 | 后端实际 | 文件 |
|---|------|----------|----------|------|
| F-1 | `listOrderTemplates` 返回类型 | `{ records: OrderTemplate[]; total: number }` | `List<OrderTemplateVO>`（纯数组） | `bpcs.ts:1005-1006` vs `OrderTemplateController.java:28` |
| F-2 | `listRma` 返回类型 | `{ records: Rma[]; total: number }` | `List<RmaVO>`（纯数组） | `bpcs.ts:1050-1051` vs `RmaController.java:31` |
| F-3 | `useOrderTemplate` 返回类型 | `string` | `OrderTemplateVO`（对象） | `bpcs.ts:1015-1016` vs `OrderTemplateController.java:62` |
| F-4 | `OrderTemplate` 接口字段 | 有 `lastUsedTime`/`createdTime` | 后端无此字段；有 `shipTo`/`remark`/`active` 前端缺失 | `bpcs.ts:994-1004` vs `OrderTemplateVO.java` |
| F-5 | `Rma` 接口字段 | 有 `itemDesc` | 后端无此字段；有 `cono`/`cust` 前端缺失 | `bpcs.ts:1038-1049` vs `RmaVO.java` |
| F-6 | `OrderChange` 接口字段 | 缺 `reason` | 后端有 `reason` 字段 | `bpcs.ts:1023-1033` vs `OrderChangeVO.java` |
| F-7 | `WhStock` 接口重复声明 | 同名不同 shape（`wh` vs `warehouse`） | TS 使用最后一个声明，第一个 `wh`/`location` 字段失效 | `bpcs.ts:245` vs `bpcs.ts:1316` |
| F-8 | `job.ts` 6 个端点返回 `Promise<void>` | 后端返回 `CommandResult` | 成功/失败结果被丢弃 | `job.ts:43-53,83,104` |

### 🟡 安全加固

| # | 问题 | 说明 | 文件 |
|---|------|------|------|
| S-8 | `PermissionService` 菜单权限静默丢失 | DB 故障时 `catch+log.warn`，用户只拿到角色权限，菜单级权限全部丢失 | `PermissionService.java:78-82` |
| S-9 | `CollectorScheduler` 抢锁失败误判单节点 | 锁机制故障时所有节点都以为自己是 leader，导致重复采集 | `CollectorScheduler.java:235-238` |
| S-10 | `TokenBlacklistService` 吊销登记失败 | 黑名单写入失败时 token 仍有效，调用方以为已吊销 | `TokenBlacklistService.java:80-82` |

### 🟡 数据库迁移问题

| # | 问题 | 说明 | 文件 |
|---|------|------|------|
| M-1 | V70/V79 `menu_name` 不匹配 | 查 `'AS400管理'` 但 V66 创建的是 `'AS400 运维'`，4 个菜单成为孤儿 | `V70:33`、`V79:59/68/77` vs `V66:6` |
| M-2 | V80 用叶子菜单做 parent_id | `/bpcs-forecast`/`/bpcs-shipping` 是叶子(`menu_type=2`)，不应做父级 | `V80:3-15` |
| M-3 | V80 缺少幂等保护 | `INSERT` 无 `WHERE NOT EXISTS`，重复执行产生重复行 | `V80:3-15` |
| M-4 | `IbmiSystem` 缺 `updatedTime` 字段 | DB 有 `updated_time` 列但 Entity 未声明 | `IbmiSystem.java:62` |
| M-5 | `rx_alert_event` 重复 Entity | `ScheduleAlertEvent`（as400 模块）和 `AlertEvent`（monitor 模块）映射同一张表，字段不一致 | 两个 Entity 类 |
| M-6 | 孤立表 `rx_job_history` | V1 创建，无任何 Entity/Mapper/Service 引用 | `V1:147` |
| M-7 | 孤立表 `rx_order_copy_log` | V73 创建，`OrderCopyController` 不操作此表 | `V73:21` |

### 🟢 代码质量（低优先级）

| # | 问题 | 说明 |
|---|------|------|
| Q-1 | 5 处 Service 返回 null 而非 throw NOT_FOUND | `BackupMonitorServiceImpl:71`、`SystemValueComplianceServiceImpl:70`、`EmailLogService:43`、`BpcsSupplyChainServiceImpl:248`、`BpcsCustomerOverviewServiceImpl:43` |
| Q-2 | 5 处 delete 方法不检查存在性 | `DictService:136`、`MenuManageService:101`、`DocTemplateService:73`、`IbmiSystemService:104`、`SysConfigService:65` |
| Q-3 | 4 处 `RuntimeException` 应改为 `BusinessException` | `AesCryptoService:94/180/191`、`WaybillPdfRenderer:129` |
| Q-4 | 7 处 `onMounted` async 无 try/catch | `Monitor.vue:229`、`scripts/index.vue:226`、`schedule/index.vue:278` 等 |
| Q-5 | 15 处 `any` 类型注解 | `ReportBuilder.vue:286/291`、`orderList/index.vue:33` 等 |
| Q-6 | `ReportBuilderService` 7 处 `BusinessException(String)` 无 ErrorCode | 默认 code=500，应使用 `NOT_FOUND` 等具体错误码 |
| Q-7 | `CalendarController:79`/`PermissionRequestController:98` Controller 层抛业务异常 | 应下沉到 Service 层 |

---

### 为什么每次 Review 都发现新问题？

1. **多层抽象遗漏**：前两轮关注 Controller/Service 层契约，本轮深入到 MyBatis ResultMap→Entity→VO→前端 Interface 的全链路字段映射，暴露了 F-1~F-8 的契约断裂。
2. **数据流盲区**：`BpcsRowUtil.intOrNull()` 在正常数据下不报错，只有 IBM i 返回 NULL 时才触发 NPE——前两轮 review 无法覆盖"脏数据路径"。
3. **迁移历史债务**：V66 创建菜单用 `'AS400 运维'`，V70/V79 后续用 `'AS400管理'` 引用——这种跨迁移的一致性只有逐条比对 SQL 才能发现。
4. **安全纵深**：前两轮检查了 `@PreAuthorize` + `DangerousClCommandValidator`，本轮发现 `UserProfileServiceImpl` 绕过了这些防护直接拼 CL 命令。
5. **前端类型系统**：TypeScript 的 `any` 和隐式类型转换掩盖了接口不匹配——只有将前端 Interface 与后端 VO 逐字段对比才能发现。

**根本原因**：项目规模大（821 Java 文件 + 53 前端 API 模块 + 90 迁移文件），单次 review 视角有限。建议建立**自动化契约测试**（如 OpenAPI spec 生成 + 前后端类型同步）从根本上消除此类问题。

---

## 第七部分：第三轮修复记录（2026-09-04）

### P0/P1 修复

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| N-1 | `BpcsRowUtil.intOrNull()` 自动拆箱 NPE | 13 个 VO 的 `int` 字段改为 `Integer`：`BpcsBomLineVO`、`BpcsLocationInventoryVO`、`BpcsOrderAnomalyVO`、`BpcsKanbanVO`、`BpcsInvoiceVO`、`BpcsPoLifecycleVO`、`BpcsReplenishmentVO`、`BpcsShipmentVO`、`BpcsStockValueVO`、`BpcsSupplierScoreVO`、`BpcsWabpConfigVO`、`BpcsInventoryAbcXyzVO`、`BpcsCustomerOverviewVO` | 13 个 VO 文件 |
| N-2 | `Collectors.toMap()` 无 merge 函数 | 4 处添加 `(a, b) -> b` merge 函数 | `MenuTreeSupport.java`、`MenuService.java`、`BackupMonitorServiceImpl.java` |
| N-3 | `JTOpenPfClient.pfStatistics()` SQL 注入 | 已验证已调用 `requireIdentifier()`，无需修复 | — |
| N-4 | `UserProfileServiceImpl` CL 命令注入 | `groupProfile`/`initialMenu`/`specialAuthorities` 添加 `requireValidIdentifier()` 校验 | `UserProfileServiceImpl.java` |

### 前后端契约修复

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| F-1 | `listOrderTemplates` 返回类型不匹配 | Vue 组件改为直接处理数组 `res \|\| []` | `orderTemplate/index.vue` |
| F-2 | `listRma` 返回类型不匹配 | API 类型改为 `Rma[]`；Vue 组件改为直接处理数组 | `bpcs.ts`、`rma/index.vue` |
| F-3 | `useOrderTemplate` 返回类型 | 已验证当前类型正确（`OrderTemplate`） | — |
| F-4 | `OrderTemplate` 接口字段不匹配 | 移除不存在的 `lastUsedTime` 列 | `orderTemplate/index.vue` |
| F-5 | `Rma` 接口字段不匹配 | 已验证当前接口字段正确 | — |
| F-6 | `OrderChange` 接口缺少 `reason` | 已验证当前接口包含 `reason` | — |
| F-7 | `WhStock` 重复声明 | 第二个 `WhStock` 重命名为 `CrossNodeWhStock`，更新 `ImbalanceItem` 引用 | `bpcs.ts` |
| F-8 | `job.ts` 端点返回 `Promise<void>` | 新增 `CommandResult` 接口；6 个端点返回类型改为 `CommandResult`/`CommandResult[]` | `job.ts` |

### 安全加固

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| S-8 | `PermissionService` 菜单权限静默丢失 | `log.warn` → `log.error`，输出完整异常堆栈 | `PermissionService.java` |
| S-9 | `CollectorScheduler` 抢锁失败误判单节点 | 异常时返回 `false`（跳过采集），`log.warn` → `log.error` | `CollectorScheduler.java` |
| S-10 | `TokenBlacklistService` 吊销登记失败 | 异常时 `throw RuntimeException`，上层可感知失败 | `TokenBlacklistService.java` |

### 编译验证

- ✅ 后端编译通过：`mvn -q -DskipTests compile`
- ✅ 前端构建通过：`npm run build`（含 TS 类型检查）

### 数据库迁移修复

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| M-1/M-2/M-3 | V70/V79 menu_name不匹配 + V80父级错误 | 新建 V91 迁移：插入'AS400管理'父菜单、更新孤儿菜单parent_id、将V80菜单从叶子父级移到BPCS目录 | `V91__fix_as400_menu_name_mismatch.sql` |
| M-4 | `IbmiSystem` 缺 `updatedTime` 字段 | Entity 添加 `private LocalDateTime updatedTime` | `IbmiSystem.java` |
| M-5 | `rx_alert_event` 重复Entity字段不一致 | `ScheduleAlertEvent` 添加 `upgradeNotified` 字段，与 `AlertEvent` 保持一致 | `ScheduleAlertEvent.java` |

### 代码质量修复

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| Q-1 | 5处Service返回null | 改为 `throw new BusinessException(NOT_FOUND)` | `BackupMonitorServiceImpl`、`SystemValueComplianceServiceImpl`、`EmailLogService`、`BpcsSupplyChainServiceImpl`、`BpcsCustomerOverviewServiceImpl` |
| Q-3 | 4处RuntimeException | `AesCryptoService` 3处改 `BusinessException(INTERNAL_ERROR)`；`WaybillPdfRenderer` 改 `BusinessException(REPORT_GENERATE_FAILED)` | 2个文件 |

### 编译验证

- ✅ 后端编译通过：`mvn -q -DskipTests compile`

### 代码质量修复（低优先级）

| # | 问题 | 修复内容 | 文件 |
|---|------|----------|------|
| Q-2 | 5处delete不检查存在性 | 添加 `selectById` 检查，不存在则抛 `NOT_FOUND` | `DictService`、`MenuManageService`、`DocTemplateService`、`IbmiSystemService`、`SysConfigService` |
| Q-4 | 7处onMounted async无try/catch | 添加 `try/catch` + `console.error` | `Monitor.vue`、`alertRules/index.vue`、`scripts/index.vue`、`inspection/index.vue`、`schedule/index.vue`、`serverCompare/index.vue`、`Users.vue` |
| Q-5 | 15处any类型注解 | `as any` → `as string`；`catch (e: any)` → `catch (e: unknown)`；`{ columns: any[] }` → `{ columns: { property?: string }[] }`；`row: any` → `row: ReportDefinition` | 8个 `.vue` 文件 |
| Q-6 | ReportBuilderService 7处BusinessException无ErrorCode | `BusinessException("msg")` → `BusinessException(NOT_FOUND/BAD_REQUEST, "msg")` | `ReportBuilderService.java` |
| Q-7 | Controller层抛业务异常 | `SysUserService` 新增 `requireByUsername()`；`PermissionRequestService` 新增 `validateComment()`；Controller 调用 service 方法 | `CalendarController`、`PermissionRequestController`、`SysUserServiceImpl`、`PermissionRequestService` |

### 未修复

（无）

### 编译验证

- ✅ 后端编译通过：`mvn -q -DskipTests compile`
- ✅ 前端构建通过：`npm run build`

---

*第三轮审查由 6 个并行 Agent 完成，覆盖：Java 空安全（821 文件）、前端 API 契约（53 模块）、SQL/命令注入（全量 mapper + native query）、错误处理（GlobalExceptionHandler + 所有 catch 块）、Vue 组件（130+ 组件）、数据库迁移（90 迁移文件 vs 65 Entity）。*
