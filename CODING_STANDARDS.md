# RXAS400ADM 编码规范 v1.0

> **唯一权威来源**。AGENTS.md 和 SKILL.md 中与此文档冲突的，以本文为准。
> 所有新代码必须遵循；存量遗留代码在修改时逐步合规。

---

## 提交前自检清单（必跑）

```
- [ ] mvn -q -DskipTests compile        后端编译
- [ ] cd frontend && npm run build       前端类型检查 + 构建
- [ ] bash scripts/verify-all.sh         八道静态门禁
```

---

## 一、命名与包结构

| # | 规则 | 示例 |
|---|------|------|
| 1.1 | 包名/工程名前缀 `com.rxas400adm` / `rxas400adm-*` | `rxas400adm-system` |
| 1.2 | Mapper 包名**必须**以 `.mapper` 结尾（`@MapperScan` 约束） | `com.rxas400adm.system.mapper` |
| 1.3 | 表名 `rx_` 前缀，MySQL 方言 | `rx_user`, `rx_menu` |
| 1.4 | 权限码格式 `{MODULE}_{ACTION}` | `DEPLOY_EXECUTE`, `JOB_VIEW` |
| 1.5 | i18n key 格式 `{namespace}.{field}`，菜单 title 用 i18n key | `$t('bpcs.kpi.totalOrders')` |
| 1.6 | Flyway 迁移命名 `V{n}__kebab-case-description.sql` | `V65__add-index-rx-user.sql` |
| 1.7 | 菜单类型值：1=目录, 2=叶子, 3=按钮, 4=Tab | 见 `rx_menu.menu_type` |

---

## 二、后端分层规则

### 2.1 Controller 层

| # | 规则 | 门禁 |
|---|------|------|
| 2.1.1 | **禁止**注入 Mapper | `check-layering.sh R1` |
| 2.1.2 | **禁止** `new QueryWrapper` / `new LambdaQueryWrapper` | `check-layering.sh R1` |
| 2.1.3 | 写接口 `@RequestBody` **必须**用 Create/Update DTO，**禁止** Entity 入参 | `check-layering.sh R2` |
| 2.1.4 | 返回类型优先 VO，**禁止**直接返回 Entity | `check-layering.sh R3` |
| 2.1.5 | 分页参数统一 `PageConstants.clampNum/clampSize`，**禁止**散落 `Math.min` 魔法值 | — |
| 2.1.6 | 每个受保护端点**必须**有 `@PreAuthorize` | — |
| 2.1.7 | 写操作**必须**加 `@OperateLog(module=..., operation=...)` | — |
| 2.1.8 | **禁止** `@Transactional`（零容忍，含 `rollbackFor` 变体） | `check-transactional.sh` |

### 2.2 Service 层

| # | 规则 |
|---|------|
| 2.2.1 | 构造器注入 `@RequiredArgsConstructor` + `private final`，**禁止** `@Autowired` 字段注入 |
| 2.2.2 | **唯一例外**：Quartz Job 类必须字段注入（SpringBeanJobFactory 反射实例化不支持构造器） |
| 2.2.3 | 多个类中出现相同工具方法时，提取到公共 utils 类（参照 `BpcsRowUtil`） |
| 2.2.4 | `AS400Client` 操作**禁止**直接 `new AS400(...)`，一律经 `AS400ClientProvider` |

### 2.3 Entity 层

| # | 规则 |
|---|------|
| 2.3.1 | `@TableName("rx_xxx")` + `@TableId(type = IdType.AUTO)` |
| 2.3.2 | 敏感字段（密码/密钥）**必须**加 `@JsonIgnore` |
| 2.3.3 | 禁止在 Entity 中添加业务逻辑（保持纯数据载体） |

### 2.4 DTO / VO 层

| # | 规则 |
|---|------|
| 2.4.1 | 查询 DTO 在 getter 中调用 `PageConstants.clampNum/clampSize` 做边界校验 |
| 2.4.2 | VO 禁止暴露自增 id、审计字段、敏感列（除非前端确需） |
| 2.4.3 | 分页返回统一用 `PageResult<T>` |

---

## 三、前端规则

### 3.1 i18n（零容忍）

| # | 规则 | 门禁 |
|---|------|------|
| 3.1.1 | **禁止硬编码任何用户可见文案**（中文/英文均须走 `$t()`） | `check-i18n.mjs` |
| 3.1.2 | 包括 Vue 模板、`<script>` 块、composable 函数中的文案 | — |
| 3.1.3 | zh-CN 和 en-US 键集**必须**完全对称 | `check-i18n.mjs` |
| 3.1.4 | 新增弹窗/表单的 `useFormDialog` `i18nPrefix` 命名空间**必须**含 `.add` 键 | `check-i18n.mjs` |
| 3.1.5 | 菜单 title 用 i18n key（如 `menu.monitor`），**禁止**中文字符串 | — |
| 3.1.6 | 语言偏好 localStorage key：`rxas400_locale`；菜单渲染用 `$t('menu.' + title)` | — |

### 3.2 模板与样式

| # | 规则 |
|---|------|
| 3.2.1 | 页面骨架：`.page-container` / `.page-container--fit` + `.search-bar` + `.table-wrapper` |
| 3.2.2 | **禁止** `style="width: 100%"`，用 `.w-full` |
| 3.2.3 | **禁止**重复定义 scoped `.toolbar/.section/.hint/.card-header`（已收敛至 `common.css`） |
| 3.2.4 | 颜色**必须**用 `var(--xxx)` CSS 变量，**禁止**硬编码色值 |
| 3.2.5 | Tab 页：`el-tabs` 直接放 `.table-wrapper` 内（勿再包 `el-card`） |
| 3.2.6 | 表格操作列按钮：`size="small"` 或 `link` 文字按钮；搜索栏按钮不加 `size` |
| 3.2.7 | el-table `#default` 插槽**禁止**窄类型标注（保持裸解构 `{ row }`，调用点断言） |
| 3.2.8 | `popper-class` **必须**全局定义（teleported to body） |
| 3.2.9 | 表格数据**必须** `size="small"` + `border` |
| 3.2.10 | 宽度工具类 `.w-90` ~ `.w-320` 替代 inline `style="width: xxxpx"` |
| 3.2.11 | 间距工具类 `.ml4` `.mt4` `.mt16` `.mb8` `.mb12` `.mb16` `.ml8` `.mr4` `.mr6` `.gap16` 替代 inline margin |
| 3.2.12 | 文本工具类 `.text-muted` `.text-danger` `.font-bold` `.count` `.section` `.hint` 替代 inline 样式 |
| 3.2.13 | 布局工具类 `.flex` `.flex-1` `.flex-row-center` 替代 inline 布局 |
| 3.2.14 | 无需分页/高度自适应页面：`.page-container`（不带 `--fit`），内部 `.search-bar` + `el-card` 等 |
| 3.2.15 | Tab 页撑满由 `.page-container--fit .table-wrapper > .el-tabs` 处理 |
| 3.2.16 | el-tree 等非 EP 表格组件插槽仍可标注 `{ data }: { data: NodeType }`（不受 el-table 插槽规则限制） |
| 3.2.17 | 无作用域的内容插槽（`<template #default>纯内容</template>`、`#header`/`#footer`/`#empty`）不需要任何处理 |
| 3.2.18 | **CRITICAL：** `SysMenu.id` 等树/表主键字段可能是 `undefined`（`id?: number`），传给 EP 组件 prop 需 `as number` 断言、`Set.has(id)` 前守卫 `data.id != null &&` |

### 3.3 Scoped 样式陷阱

> **核心原理**：父组件 `<style scoped>` 只对子组件**根元素**生效，**到不了子组件内部嵌套元素**。

| # | 规则 |
|---|------|
| 3.3.1 | 被子组件引用的类**必须**放全局块或子组件自己的 style 块 |
| 3.3.2 | 布局级全局样式放所属 layout 的 `<style>`（非 scoped）块并加注释 |
| 3.3.3 | **禁止**跨组件依赖 scoped 私有类 |

### 3.4 TypeScript

| # | 规则 | ESLint |
|---|------|--------|
| 3.4.1 | `@typescript-eslint/no-explicit-any`: **error** | `eslint.config.js` |
| 3.4.2 | `@typescript-eslint/no-unused-vars`: **error**（前缀 `_` 标记有意不用） | `eslint.config.js` |
| 3.4.3 | `vue/no-v-html`: **error**（需 DOMPurify 先清洗） | `eslint.config.js` |
| 3.4.4 | `vue/no-static-inline-styles`: **error** | `eslint.config.js` |

### 3.5 组件与 Composable（必须复用）

| # | 规则 | 说明 |
|---|------|------|
| 3.5.1 | 列表页使用 `useSmartQueryTable` composable | 含防抖/强制后端查询/本地筛选/分页/加载状态，内部封装 `useTablePage` |
| 3.5.2 | 表单弹窗使用 `useFormDialog` composable | 含 dialogVisible/isEdit/loading/formRef/form/rules/openCreate/openEdit/onSubmit + i18nPrefix 命名空间 |
| 3.5.3 | 删除操作使用 `useConfirmDelete` composable | 含 ElMessageBox.confirm → loading → API → success → refresh |
| 3.5.4 | 表格分页使用 `<AppPagination>` 组件，**禁止**手动分页 | 右对齐，含 sizes + jumper |
| 3.5.5 | 搜索栏使用 `<QueryBar>` 组件 | 含 keyword 输入/搜索重置按钮/缓存指示器/刷新图标 |
| 3.5.6 | 加载占位使用 `<RxSkeleton>` 组件 | 支持 `table`/`card`/`list` 类型，含自动过渡 |
| 3.5.7 | Blob 下载使用 `triggerBlobDownload()`，**禁止**手写下载逻辑 | — |
| 3.5.8 | 列表动画使用 `flash-pop` CSS 类 + `useFlash` composable | — |
| 3.5.9 | ECharts 使用 `useECharts` composable | 含 init/setOption/resize/dispose + unmount 自动清理 |
| 3.5.10 | 格式化使用 `utils/format.ts`（`formatMoney`/`formatSize`/`formatDate`/`formatTimestamp`） | — |
| 3.5.11 | CSS 变量读取使用 `utils/cssVar.ts`（ECharts 等需要 JS 端读取主题色时） | — |
| 3.5.12 | 密码验证使用 `utils/passwordPolicy.ts`（前端校验与后端 `PasswordPolicy.java` 同步） | — |
| 3.5.13 | WebSocket 使用 `useStompClient` composable（自动 JWT 认证/5s 重连/订阅去重/unmount 断开） | — |
| 3.5.14 | 主题切换使用 `useTheme` composable（dark/light + 5 色，localStorage 持久化） | — |
| 3.5.15 | Token 自动刷新使用 `useTokenRefresh` composable（过期前 5 分钟刷新） | — |
| 3.5.16 | 键盘快捷键使用 `useShortcuts` composable（注册/解析/匹配 + unmount 自动清理） | — |
| 3.5.17 | 数据自动刷新使用 `useAutoRefresh` composable（可配置间隔/开关/上下界） | — |
| 3.5.18 | localStorage 使用 `useStorage` composable（typed get/set + token XOR 混淆 + 命名空间 key） | — |

---

## 四、数据库与迁移

| # | 规则 | 门禁 |
|---|------|------|
| 4.1 | Flyway 管理所有表结构，**禁止**手动改表 | — |
| 4.2 | 迁移命名 `V{n}__kebab-case.sql`，版本连续唯一 | `check-migrations.mjs` |
| 4.3 | 种子数据 INSERT**必须**用 `INSERT IGNORE` / `WHERE NOT EXISTS` 保证幂等 | `check-migrations.mjs` |
| 4.4 | **CRITICAL：种子 INSERT 列名必须与当前表 DDL 完全一致**（不可假设列名不变） | — |
| 4.5 | 空迁移文件**禁止**（每个迁移至少含一条非注释语句） | `check-migrations.mjs` |
| 4.6 | 跨迁移 CREATE TABLE/INDEX 名称**禁止**重复 | `check-migrations.mjs` |
| 4.7 | MyBatis XML**禁止** `${}`，一律用 `#{}` | — |
| 4.8 | 分页用 `Page` 对象或 `PageConstants.limitClause()`，**禁止** `.last("LIMIT n")` | — |

---

## 五、安全

| # | 规则 |
|---|------|
| 5.1 | 日志**禁止**记录密码/Token/PII 等敏感数据 |
| 5.2 | CL 命令拼接参数**必须**经 `requireIdentifier()` 或白名单校验 |
| 5.3 | URL 参数**必须**经 `SsrfGuard.assertSafeUrl()` 校验（Webhook 等场景） |
| 5.4 | 响应头**禁止**直接拼接用户输入（HTTP Header 注入防护） |

---

## 六、死代码与重复

| # | 规则 |
|---|------|
| 6.1 | 提交前清理未使用的 import、变量、方法（IDE unused 警告须逐一确认） |
| 6.2 | 多个类中出现相同工具方法时，提取到公共 utils（参照 `BpcsRowUtil` 提取前例） |
| 6.3 | 注解**禁止**使用 FQN 形式（如 `@io.swagger.v3.oas.annotations.tags.Tag`），一律顶部 import + 简名引用；已有同名注解 import 时禁止重复添加 FQN 变体 |

---

## 七、Java 编码

| # | 规则 | 门禁 |
|---|------|------|
| 7.1 | **禁止**内联全限定类名，一律顶部 import + 简名引用 | `verify-all.sh` regex |
| 7.2 | 枚举/常量优先，**禁止**魔法数字 | — |
| 7.3 | `ErrorCode` 按域分段分配（见下表） | — |
| 7.4 | 统一返回 `ApiResponse{ code:0, message, data }`，code=0 成功 | — |
| 7.5 | 异常用 `BusinessException(ErrorCode.XXX, "message")` | — |
| 7.6 | Entity 查找-or-抛异常使用 `EntityUtil.require(id, name, lookup)` | — |
| 7.7 | 分页 SQL 方言差异使用 `PageConstants.limitClause()`（自动适配 MySQL `LIMIT` / DB2 `FETCH FIRST`），**禁止** `.last("LIMIT n")` | — |
| 7.8 | IBM i 标识符校验使用 `As400Identifiers.IDENTIFIER` / `As400Identifiers.JOB_NUMBER` 正则 | — |
| 7.9 | Cron 表达式预校验使用 `CronValidator.validate(expr)` | — |
| 7.10 | CL 命令黑名单校验使用 `DangerousClCommandValidator`（17 内置动词 + SPI 扩展） | — |
| 7.11 | Webhook 推送使用 `WebhookNotifier`（含 3 次重试 + SSRF 防护） | — |
| 7.12 | AES 加密使用 `AesCryptoService`（AES-256-GCM + PBKDF2），**禁止**自实现加解密 | — |
| 7.13 | 环境判断使用 `ProfileResolver.isDevLikeMode()` / `isMockMode()`，**禁止**硬编码 `"dev".equals(...)` | — |
| 7.14 | 跨模块事件使用 Spring `ApplicationEvent`（参照 `AlertRaisedEvent` / `UserPermissionGrantedEvent`） | — |

### ErrorCode 域分段

| 域 | 范围 | 示例 |
|----|------|------|
| 成功 | 0 | `SUCCESS` |
| 用户 | 10000+ | `USER_NOT_FOUND`, `LOGIN_FAILED` |
| AS400 | 20000+ | `AS400_CONNECTION_FAILED` |
| 部署 | 30000+ | `DEPLOY_APPROVAL_REQUIRED` |
| 监控 | 40000+ | `MONITOR_INSTANCE_NOT_FOUND` |
| 系统 | 50000+ | `SYSTEM_CONFIG_ERROR` |
| 报表 | 60000+ | `REPORT_GENERATION_FAILED` |
| 数据源 | 80000+ | `SOURCE_CONNECTION_FAILED` |
| 定时任务 | 90000+ | `JOB_SCHEDULE_CONFLICT` |
| 角色 | 110000+ | `ROLE_IN_USE` |
| 密码 | 120000+ | `PASSWORD_TOO_WEAK` |

---

## 八、架构与环境

| # | 规则 |
|---|------|
| 8.1 | `rxas400adm-app` 是唯一启动模块（`com.rxas400adm.Rxas400admApplication`） |
| 8.2 | `@MapperScan("com.rxas400adm.**.mapper")` — Mapper 包名必须匹配 |
| 8.3 | 跨模块依赖：security → system、as400 → common 等，新增依赖改对应模块 pom |
| 8.4 | WebSocket STOMP：`/topic/monitor/{instanceId}`、`/topic/deployment/{id}`，前端 `@stomp/stompjs` |
| 8.5 | 实时推送使用 `SimpMessagingTemplate`；定时任务/后台线程无请求头，需显式遍历服务器（参考 `CollectorScheduler`） |
| 8.6 | Flyway 配置：`validate-on-migrate: false`、`baseline-on-migrate: true` |
| 8.7 | 权限码需在 `DataInitializer.PERMISSION_CODES` 中登记（首次启动插入 `rx_permission`） |
| 8.8 | 新建写操作接口时，如无既有审计权限码，需先在 `DataInitializer` 中新增对应权限码 |
| 8.9 | 前端 API 模块统一从 `request.ts` 导出实例调用（已带 Token + `X-AS400-Server` 头注入） |
| 8.10 | 路由：`frontend/src/router/index.ts` 静态路由 + 后端 `/api/v1/auth/menu` 动态菜单（按权限码裁剪） |

### 端到端验证流程

登录 → 动态菜单 → 发布流水线（创建/审批/执行）→ 多服务器路由（`X-AS400-Server` 头）→ 监控采集 → 审计日志

---

## 九、编译与构建

| # | 规则 |
|---|------|
| 9.1 | 改完代码先 `mvn -q -DskipTests compile`（增量）验证编译，**不要每次 `mvn clean package`** |
| 9.2 | 需要跑起来用 `java -jar`（先 `taskkill //F //IM java.exe` 停旧进程；Windows 下 jar 被占用无法覆盖） |
| 9.3 | 打包分发给部署才 `mvn -DskipTests package` |
| 9.4 | 前端 `npm run build` 会同时做 TS 类型检查；改完代码先 `cd frontend && npm run build` 验证 |
| 9.5 | 端到端冒烟（后端已启动）：见下方 curl 命令 |

### 端到端冒烟 curl

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s http://localhost:8080/api/v1/auth/menu -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/as400/servers -H "Authorization: Bearer $TOKEN"
```

---

## 十、编码约定

| # | 规则 |
|---|------|
| 10.1 | 所有源文件（`.java`/`.vue`/`.ts`/`.sql`/`.yml`）**必须** UTF-8 无 BOM |
| 10.2 | MySQL 命令行**必须**加 `--default-character-set=utf8mb4`（⚠️ Windows 下 MySQL 客户端默认 GBK，UTF-8 的 SQL 文件会双编码写入导致乱码） |
| 10.3 | 创建/编辑文件**必须**用文件编辑工具，**禁止** `cat`/`echo` 重定向（命令行重定向在非 UTF-8 locale 下会写入错误编码） |
| 10.4 | 后端日志中文在 Git Bash 下正常；如遇乱码检查 `file.encoding`，本地启动加 `-Dfile.encoding=UTF-8` |
| 10.5 | 每次改动后跑 `mvn -q -DskipTests compile` + `npm run build` 验证 |

### Windows 中文编码陷阱（必读）

Windows 中文环境默认编码为 GBK（CP936），以下场景会触发双编码乱码：

| 场景 | 原因 | 正确做法 |
|------|------|----------|
| `mysql -uroot -p < data.sql` | MySQL 客户端按 GBK 读取 UTF-8 文件 → 写入时再转 UTF-8 → 中文双编码 | 加 `--default-character-set=utf8mb4` |
| `echo "中文" > file.sql` | PowerShell/CMD 按系统默认编码输出重定向 | 用文件编辑工具 |
| `cat file.sql \| mysql` | 管道传递的字节流按 locale 解码，Git Bash/CMD locale 非 UTF-8 | 加 `--default-character-set=utf8mb4` |

验证当前编码：`mysql -uroot -p -e "SHOW VARIABLES LIKE 'character_set%';"` — 确保 `character_set_client` / `character_set_results` 均为 `utf8mb4`。

---

## 十一、Git 与工作流

| # | 规则 |
|---|------|
| 11.1 | 提交前 `git status` + `git diff`，**禁止** `git add -A` |
| 11.2 | **禁止** push/rebase（除非用户明确要求） |
| 11.3 | 启用提交前门禁：`git config core.hooksPath .githooks`（提交自动跑八道静态门禁 + 后端 `mvn test`，失败即阻断） |
| 11.4 | 紧急跳过：`SKIP_VERIFY_ALL=1 git commit`；仅跳过后端单测：`SKIP_BACKEND_TESTS=1 git commit` |
| 11.5 | 多代理环境：先检查分支状态再动文件 |
| 11.6 | PR 模板：`.github/PULL_REQUEST_TEMPLATE.md`（门禁自检清单 + 两道 review 结论，CI 红即不合，禁止合并） |

---

## 十二、新增模块 14 步流程

1. DDL — Flyway 迁移文件 `V{n}__kebab-case.sql`
2. Entity — `@TableName` + `@TableId(AUTO)`；敏感字段加 `@JsonIgnore`
3. Mapper — `.mapper` 包，继承 `BaseMapper<Entity>`
4. DTO — `dto/` 包（Create/Update/Query）；查询 DTO getter 中调 `PageConstants.clampNum/clampSize`
5. VO — `vo/` 包，**禁止**直接暴露 Entity
6. Service 接口
7. ServiceImpl — `@RequiredArgsConstructor` 构造器注入（Quartz Job 例外：字段注入）
8. Controller — `@RestController` + `@PreAuthorize` + `@OperateLog`
9. 权限码 — 在 `DataInitializer.PERMISSION_CODES` 注册新权限码
10. 前端 API — `frontend/src/api/xxx.ts`
11. Vue 页面 — `frontend/src/views/{module}/xxx.vue`；使用 §3.5 列出的 composable/组件
12. i18n — zh-CN + en-US **对称**新增（包括菜单 title key）
13. 验证 — `mvn test` + `npm run build` + `verify-all.sh`
14. 文档 — 更新本文件（如涉及新约定）

---

## 十三、门禁速查

| 门禁 | 检查内容 | 失败动作 |
|------|----------|----------|
| `check-layering.sh` | Controller 分层规则 R1-R3 | exit 1 |
| `check-transactional.sh` | `@Transactional` 零容忍 | exit 1 |
| `check-frontend-slots.sh` | el-table 插槽类型 | R1 警告 / R2 exit 1 |
| `check-template-join.mjs` | CRLF 拼行防护 | exit 1 |
| `check-template-classes.mjs` | 模板 class 样式定义 | exit 1 |
| `check-i18n.mjs` | i18n key 对称性 | exit 1 |
| `check-v38-consistency.mjs` | V38 种子一致性 | exit 1 |
| `check-migrations.mjs` | 迁移结构一致性 | exit 1 |
| `verify-all.sh` | 一键聚合以上全部 | exit 1 |

---

## 十四、常见违规 Top 10

| # | 违规 | 正确做法 |
|---|------|----------|
| 1 | Controller 里 `new LambdaQueryWrapper` | 下沉到 Service 层 |
| 2 | 返回 Entity 给前端 | 用 VO 包装 |
| 3 | 硬编码中文/英文文案 | `$t()` + i18n 双语 |
| 4 | 内联 FQN `new com.xxx.Yyy()` | 顶部 import + 简名 |
| 5 | 遗漏 `@OperateLog` | 写操作必加 |
| 6 | 遗漏 `@PreAuthorize` | 受保护端点必加 |
| 7 | 种子 INSERT 列名与 DDL 不一致 | 逐列核对建表语句 |
| 8 | `style="width: 100%"` | 用 `.w-full` |
| 9 | 多处重复工具方法 | 提取到公共 utils |
| 10 | 未清理 unused import/变量 | 提交前清理 |

---

## 十五、IBM i（AS400）开发规范

### 13.1 连接与访问

| # | 规则 |
|---|------|
| 13.1.1 | 业务代码**禁止**直接 `new AS400(...)` 或 `new AS400ConnectionPool()`，一律经 `AS400Client` 接口 |
| 13.1.2 | 通过 `AS400ClientProvider.getClient()` 获取客户端（自动按服务器 ID 路由） |
| 13.1.3 | Mock 开发走 `MockAS400Client`（`spring.profiles.active=mock`），生产走 `JTOpenAS400Client`（`prod`） |
| 13.1.4 | 多服务器路由：前端发 `X-AS400-Server: <id>` 头 → `As400ServerIdInterceptor` → ThreadLocal → Provider |

### 13.2 数据访问

| # | 规则 |
|---|------|
| 13.2.1 | QSYS2 系统表**只读**（`SELECT` only），**禁止** DML 操作 |
| 13.2.2 | CL 命令执行使用 `AS400Client.executeCommand()`，参数**必须**经 `As400Identifiers.IDENTIFIER` 正则校验 |
| 13.2.3 | CL 命令黑名单校验使用 `DangerousClCommandValidator`（17 内置动词 + SPI 扩展） |
| 13.2.4 | BPCS/ERP 行级操作使用 `BpcsRowUtil` 工具类（`pickStr`/`strEq`/`decOrNull` 等），**禁止**各 Service 重复实现 |
| 13.2.5 | IBM i 日期格式（YYYYMMDD/CYYMMDD）解析使用 `BpcsDateUtil.toLocalDate()` |

### 13.3 SQL 兼容性

| # | 规则 |
|---|------|
| 13.3.1 | 分页 SQL 使用 `PageConstants.limitClause()`（自动适配 MySQL `LIMIT n` / DB2 for i `FETCH FIRST n ROWS ONLY`） |
| 13.3.2 | **禁止** `.last("LIMIT n")`（硬编码 MySQL 语法，DB2 for i 不兼容） |
| 13.3.3 | DB2 for i 物理表默认不开启 Journaling；未开日志的表执行 `COMMIT`/`ROLLBACK` 会报 **SQL7008** |

### 13.4 环境与部署

| # | 规则 |
|---|------|
| 13.4.1 | 本地开发用 `mock` profile（无需真实 IBM i），**禁止**本地直连生产 AS400 |
| 13.4.2 | 生产启动**必须** `prod` profile + 设 `RXAS400_JWT_SECRET` 环境变量（否则 `StartupGuard` 拒启） |
| 13.4.3 | 生产环境变量（`rxas400.env`）：`RXAS400_JWT_SECRET` / `RXAS400_DB_HOST` / `RXAS400_DB_USER` / `RXAS400_DB_PASSWORD` |
| 13.4.4 | 部署路径：`/QOpenSys/opt/rxas400/{backend,frontend}`（AS400 PASE / Linux LPAR） |
| 13.4.5 | 部署使用 `bash scripts/deploy-to-as400.sh`（自动构建 + SCP 上传 + 重启） |
| 13.4.6 | 后端管理使用 `bash scripts/start-backend.sh [start|stop|restart|status]` |
| 13.4.7 | **禁止**在生产 AS400 上手动运行 `java -jar`（必须通过脚本管理，确保 env 加载 + PID 追踪） |

### 13.5 多服务器配置

| # | 规则 |
|---|------|
| 13.5.1 | 服务器注册在 `rx_ibmi_system` 表（由 Flyway 种子初始化） |
| 13.5.2 | 前端顶栏服务器切换 → 发送 `X-AS400-Server` 头 → 后端按 ID 路由到对应 `AS400Client` |
| 13.5.3 | 新增 AS400 服务器：INSERT `rx_ibmi_system` + 配置连接参数，无需改代码 |

---

## 十六、常见坑与对策

| # | 坑 | 现象 | 对策 |
|---|-----|------|------|
| 16.1 | Mapper 不在 `.mapper` 包 | 启动报 `Mapper` 找不到 | 包名**必须**以 `.mapper` 结尾 |
| 16.2 | MySQL 保留字作列名 | DDL 或查询报语法错（`system`） | 列名避开保留字（用 `system_name`） |
| 16.3 | 瞬态字段跨事务 | 重新 `get(id)` 后瞬态字段为 null → NPE | 需要时显式重设（参考 `DeploymentService.execute` 的 `setStepPlan`） |
| 16.4 | MyBatis Plus insert 重载歧义 | Mockito `any()` 匹配报错 | 用 `ArgumentCaptor` 消歧 |
| 16.5 | jar 被占用 | 打包报错无法覆盖 | 先 `taskkill //F //IM java.exe` |
| 16.6 | 定时任务线程无请求上下文 | `ThreadLocal` 服务器为空 → 任务静默跳过 | 任务内显式遍历服务器（参考 `CollectorScheduler`） |
| 16.7 | Flyway 迁移列名与种子不一致 | 种子 INSERT 报列不存在 | 逐列核对建表语句列名（实测 V60-V64 修复 4 个迁移文件） |
| 16.8 | `.last("LIMIT n")` 硬编码分页 | DB2 for i 不兼容 SQL 语法 | 用 `PageConstants.limitClause()` |
| 16.9 | CRLF 批量替换吞换行 | `<template #default>` 与前一行 `el-*` 开标签拼行 | 用含换行的多行 `oldString` |
| 16.10 | 脚本正则 `$]` 被环境展开 | 正则语法错误 | 用 `[A-Za-z0-9_]` / perl `[\w]` |

---

## 十七、文档站（VitePress）

| # | 规则 |
|---|------|
| 17.1 | `docs/` 下独立 VitePress 站点（自带 `package.json`），`config.ts` 用 `withMermaid()` 包裹 |
| 17.2 | Trae 审计报告按章拆页：`npm run split:trae`，**源报告修改后必须重跑拆页**，否则站点内容过期 |
| 17.3 | 站点收录白名单：首页 + `review/trae/*` + 合订本 + 上手指南 + 避坑指南；其余历史遗留 md 在 `config.ts` 的 `srcExclude` 排除 |
| 17.4 | 合订本/其他被站点收录的 md 中，HTML 尖括号**必须**包反引号（如 `` `List<Metric>` ``），否则 vue 编译报未闭合标签 |
| 17.5 | **不要**往 `docs/` 随便丢 `.md`，会被 VitePress 当页面编译（未闭合 HTML 标签会直接 build 失败） |

---

*v1.0 | 2026-08-27 | 基于两轮完整 Code Review 制定*
