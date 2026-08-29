# AGENTS.md — RXAS400ADM（IBM i 运维管理平台）

> **⚠️ 编码规范权威文档：[`CODING_STANDARDS.md`](CODING_STANDARDS.md)（176 条规则，17 章节）。**
> **opencode 每次写代码前必须先读取该文件**（`Read CODING_STANDARDS.md`），本文档仅作架构补充。

Spring Boot 3.3 (Java 17) 多模块 Maven + Vue 3 (TypeScript, Vite) 前后端分离项目。
包名/工程名：`com.rxas400adm` / `rxas400adm-*`；数据库：MySQL 8，库名 `rxas400adm`。
模块：`common / system / security / as400 / source / monitor / app`（deploy 已于 V30 下线、compile 已于 V56 下线移除，见 `backend/pom.xml`）。
前端：Vue3 + TS + Vite + Element Plus + Pinia + vue-i18n（zh-CN / en-US）+ ECharts + @stomp/stompjs。

> 本文件借鉴旧项目 `D:\vueprojects\RXAS400\AGENTS.md` 与 `.opencode/skills/rx-admin-dev/SKILL.md` 中
> 适用于本项目的约定，并适配当前技术栈（Spring Security 6 + JWT、MySQL、TS 前端、多服务器 Provider）。

## 快速开始

| 命令 | 说明 |
| --- | --- |
| `cd backend && mvn -q -DskipTests compile` | 快速编译检查（增量，最快） |
| `cd backend && mvn test` | 运行全部单元测试 |
| `cd backend && mvn -DskipTests package` | 打包（产出 `rxas400adm-app/target/rxas400adm-app-*.jar`） |
| `java -jar backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar --spring.datasource.password=root --spring.profiles.active=mock` | 本地启动（后端 8080；**mock 档**：AS400 走 `MockAS400Client` 假数据，菜单/用户/权限等仍读本地 docker MySQL，无需真实 IBM i 即可联调） |
| `RXAS400_JWT_SECRET=<32+字节随机值> java -jar backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar --spring.datasource.password=root --spring.profiles.active=prod` | 生产启动（**必须** `prod` 档 + 设 `RXAS400_JWT_SECRET` 环境变量，否则 `StartupGuard` 拒启；`prod` 下 AS400 走真实 `JTOpenAS400Client` 连 IBM i）。推荐直接用 `scripts/start-backend.sh`（已内置 `--spring.profiles.active=prod` 并自动加载 env） |
| `cd frontend && npm run dev` | 前端 dev server（5173，代理 /api、/ws 到 8080） |
| `cd frontend && npm run build` | 前端构建验证 |
| `cd docs && npm run dev` | 审计文档站（VitePress，默认 5174）：Trae 报告按章拆页 + 合订本单页 + 全文搜索 |
| `cd docs && npm run split:trae && npm run build` | 重新拆页 + 构建文档站（源报告修改后必须重跑拆页） |

默认演示账号：`admin` / `admin123`。API 文档：`http://localhost:8080/swagger-ui.html`（springdoc）。
本机 MySQL：`root` / `root` @ `localhost:3306`。数据库已存在且由 Flyway 管理（`backend/rxas400adm-app/src/main/resources/db/migration/`），**不要手动改表结构，用新迁移文件**。

## 审计文档站（VitePress，2026-08-15 落地）

`docs/` 下独立 VitePress 站点（自带 `package.json`，依赖 `vitepress` + `vitepress-plugin-mermaid` + `mermaid`；`config.ts` 用 `withMermaid()` 包裹，站内 `mermaid` 代码块直接渲染，如 `flowcharts/modules-mermaid-source.md`）：

> 流程图编辑 / 新模块画图 / 挂系统侧边栏菜单：见根目录《模块流程图与菜单接入指南.md》（含 iframe 内嵌与外链两种菜单方案及门禁影响）。

- **Trae 审计报告按章拆页**：`npm run split:trae` 把 `Trae-RXAS400ADM-2026-08-15.md` 按 `## ` 章标题拆成 `docs/review/trae/{nn}-{slug}.md`（19 页，附录 → 99-）；**源报告修改后必须重跑拆页**，否则站点内容过期。
- **合订本保持单页全文**（线性 review 日志，拆页反而不利导航）；全文搜索（`provider: local`）覆盖站内所有页面。
- **站点收录白名单**：首页 + `review/trae/*` + 合订本 + 上手指南 + 避坑指南；其余历史遗留 md（`项目开发步骤追踪.md` 等已并入合订本）在 `config.ts` 的 `srcExclude` 排除——**不要**往 `docs/` 随便丢 .md，会被 VitePress 当页面编译（未闭合 HTML 标签会直接 build 失败，实测踩过：`List<Metric>` 未包反引号 → `Element is missing end tag`）。
- 合订本/其他被站点收录的 md 中，**HTML 尖括号必须包反引号**（如 `List<Metric>` → `` `List<Metric>` ``），否则 vue 编译报未闭合标签。

## ⚠️ Windows 中文编码警告（务必阅读）

中文 Windows 下 MySQL 客户端默认 GBK 编码，UTF-8 的 SQL 文件会双编码写入导致乱码：

```bash
# ✅ 正确：显式指定 UTF-8
mysql -uroot -proot --default-character-set=utf8mb4 -D rxas400adm -e "SHOW TABLES;"
# ❌ 错误：默认 GBK，中文必乱码
mysql -uroot -proot -D rxas400adm -e "SOURCE script.sql"
```

- 所有源文件（`.java`/`.vue`/`.ts`/`.sql`/`.yml`）必须 **UTF-8 无 BOM**
- 命令行工具（`cat`/`echo` 重定向等）只用于查看；**创建/编辑文件一律用文件编辑工具**
- 后端日志中文在 Git Bash 下正常；如遇乱码检查 `file.encoding`，本地启动加 `-Dfile.encoding=UTF-8`

## 编译/运行工作流（避免浪费时间）

- 改完代码先 `mvn -q -DskipTests compile`（增量）验证编译，**不要每次 `mvn clean package`**
- 需要跑起来用 `java -jar`（先 `taskkill //F //IM java.exe` 停旧进程；Windows 下 jar 被占用无法覆盖）
- 打包分发给部署才 `mvn -DskipTests package`
- 前端 `npm run build` 会同时做 TS 类型检查；改完代码先 `cd frontend && npm run build` 验证
- 端到端验证：登录 → 动态菜单 → 发布流水线（创建/审批/执行）→ 多服务器路由（`X-AS400-Server` 头）→ 监控采集 → 审计日志

## 架构 must-knows

- **多模块 Maven**：`rxas400adm-app` 是唯一启动模块（`com.rxas400adm.Rxas400admApplication`），`@MapperScan("com.rxas400adm.**.mapper")`
- **统一返回**：`ApiResponse{ code:0, message, data }`，code=0 成功；错误码见 `ErrorCode` 枚举（按域分段）
- **认证**：Spring Security 6 + JWT（`rxas400adm-security`），`@PreAuthorize` 权限码（如 `DEPLOY_EXECUTE`、`DEPLOY_APPROVE`）
- **审计**：写操作加 `@OperateLog(module=..., operation=...)` 注解，AOP 自动写 `rx_audit_log`
- **多 AS400 服务器**：前端发 `X-AS400-Server: <id>` 头 → `As400ServerIdInterceptor` → `As400ServerContextHolder`(ThreadLocal) → `AS400ClientProvider` 按服务器取客户端（mock 模式开发 / JT400 模式生产）
- **IBM i 访问抽象**：业务代码不允许直接 `new AS400(...)`，一律经 `AS400Client` 接口（`MockAS400Client` / `JTOpenAS400Client`）
- **实时推送**：WebSocket STOMP（`/topic/monitor/{instanceId}`），前端 `@stomp/stompjs`
- **数据库**：Flyway 管理（user/role/permission/metric/alert/audit/config/ibmi_system/...；deploy 相关表已于 V30 下线）

## 新增模块完整流程（14 步，参照旧项目 rx-admin-dev skill 精简）

1. **DDL**：新 Flyway 迁移文件（`V{n}__*.sql`，MySQL 方言，`rx_` 前缀表名）
2. **Entity**：`@TableName("rx_xxx")` + MyBatis Plus 注解，`@TableId(type = IdType.AUTO)`
3. **Mapper**：继承 `BaseMapper<Entity>`，包名 **必须** 以 `.mapper` 结尾（`@MapperScan("com.rxas400adm.**.mapper")` 约束）
4. **DTO**：`dto/` 包（Create/Update/Query）；分页参数用 `@RequestParam current/size`，边界统一走 `PageConstants.clampNum/clampSize`（`common.constants.PageConstants`，MAX_PAGE_SIZE=100，禁止散落 `Math.min(100, ...)` 魔法值）
5. **VO**：`vo/` 包，**禁止直接暴露 Entity 给前端**
6. **Service 接口**：方法参数用 DTO，返回 `ApiResponse<T>` 或分页数据
7. **ServiceImpl**：`@RequiredArgsConstructor` 构造器注入（`private final`），**禁止 `@Autowired` 字段注入**。⚠️ 唯一例外：Quartz Job 类（`ScheduleQuartzJob`/`ReportScheduleQuartzJob`）必须字段注入——SpringBeanJobFactory 反射实例化 Job 后仅 `autowireBean()`（支持字段/Setter，不支持构造器），构造器注入会导致运行时依赖永远 null、任务静默跳过（类 javadoc 已注明）
8. **Controller**：`@RestController` + `@RequestMapping("/api/v1/xxx")`，`@PreAuthorize` 权限码
9. **写操作**：`@OperateLog`（如无既有审计需先建对应权限码）。⚠️ 项目已取消全部 `@Transactional`（零容忍，含 `rollbackFor` 变体），详见 `CODING_STANDARDS.md` §2.1.8
10. **前端 API 模块**：`frontend/src/api/xxx.ts`，统一从 `request.ts` 导出实例调用
11. **Vue 页面**：`frontend/src/views/{module}/xxx.vue`，`<script setup lang="ts">`
12. **i18n**：文案加到 `frontend/src/i18n/lang/zh-CN.ts` + `en-US.ts`（**禁止硬编码中文**）
13. **路由**：`frontend/src/router/index.ts` 静态路由 + 后端 `/api/v1/auth/menu` 动态菜单（菜单项 title 用 i18n key，如 `menu.monitor`）
14. **验证**：`mvn test` + `npm run build` + 端到端 curl

## 前端页面布局规范（参照旧项目 RXAS400/ui，样式统一在 `frontend/src/styles/common.css`）

所有页面统一骨架，**禁止**自造局部类名/内联样式重复实现公共样式：

```html
<div class="page-container page-container--fit">   <!-- 列表页用 --fit 撑满可视高度，表格+分页吸底 -->
  <div class="search-bar">                          <!-- 筛选/操作按钮行：紧凑 28px 控件、默认 input/select 宽 200px -->
    <el-input v-model="keyword" clearable @keyup.enter="load" />
    <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
  </div>
  <div class="table-wrapper">                       <!-- 白底卡片圆角容器；--fit 下内部表格自动撑满 -->
    <el-table :data="rows" v-loading="loading" size="small" border>…</el-table>
    <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
  </div>
  <!-- 弹窗/抽屉放 page-container 内、table-wrapper 之后 -->
  <el-dialog …>…</el-dialog>
</div>
```

- **Tab 页**（job/Users 等）：`el-tabs` 直接放 `.table-wrapper` 内（勿再包 el-card），撑满由 `.page-container--fit .table-wrapper > .el-tabs` 处理
- **通用工具类**：`.w-full`（弹窗表单控件占整行，代替 `style="width:100%"`）、`.flex-1`、`.mr4/.ml8/.mb16` 等间距类、`.section`（小节标题）、`.hint`、`.count`、`.text-muted/.text-danger`
- **硬编码红线**：① 禁止 `style="width: 100%"`（用 `.w-full`）；② 禁止重复定义 scoped `.toolbar/.section/.hint/.card-header` 等（已收敛至 common.css）；③ 颜色一律 `var(--xxx)` 主题变量
- **样式作用域归属（scoped 透传陷阱，2026-08-16 实测）**：父组件 `<style scoped>` 只对子组件**根元素**生效，**到不了子组件内部嵌套元素**——被 `check-template-classes` v2 抓到真实 bug（`LayoutHeaderActions` 的 `header-action-btn`/`theme-color-dot`/`user-avatar` 等样式整体丢失，8 个按钮 `display:block`/16px/`cursor:auto`）。约定：**被子组件引用的类一律放全局块或子组件自己的 style 块**；布局级全局样式集中在所属 layout 的 `<style>`（非 scoped）块并加注释说明原因（参照 `frontend/src/layout/index.vue` 全局块头注释「父组件 scoped 到不了子组件内部，故放全局块」）；禁止跨组件依赖 scoped 私有类（如 `ApprovalPanel` 曾用 `Users.vue` 的私有 `.muted`，已改全局 `.text-muted`）
- **表格/树插槽行类型收窄（2026-08-23 修订，vue-tsc 实测推翻旧约定）**：**el-table-column 的 `#default` 禁止窄类型标注** `<template #default="{ row }: { row: RowType }">`——Element Plus 将槽参类型固定为 `{ row: DefaultRow; column: ...; $index: number }`，函数参数逆变检查下窄类型必然 `TS2322`（交叉索引签名也救不了）。正确写法：**保持裸解构 `{ row }`，在调用点断言收窄** `@click="openEdit(row as SysRole)"`（全库 29 文件 80+ 处已按此模式修复并使 `npm run build` 恢复绿色）。el-tree 等非 EP 表格组件插槽仍可标注 `{ data }: { data: NodeType }`。行类型优先复用 api 层已有接口（`UserVO`/`JobInfo`/`SpoolFile`/`SysMenu` 等）；API 返回类型必须与后端 JSON 形状一致（大写列名如实声明，参照 `api/job.ts` 的 `SpoolFile`/`JobLogRow`）。门禁：`bash scripts/check-frontend-slots.sh` R1 对裸插槽仅警告不阻断（R2 悬空 RowType 检查继续生效）；`npm run lint` 内含 CRLF 拼行防护（`node scripts/check-template-join.mjs`，CRLF 文件批量替换必须用含换行的多行 oldString）。**坑**：① `SysMenu.id` 等树/表主键字段可能是 `undefined`（`id?: number`），传给 EP 组件 prop 需 `as number` 断言、`Set.has(id)` 前守卫 `data.id != null &&`；② 无作用域的内容插槽（`<template #default>纯内容</template>`、`#header`/`#footer`/`#empty`）不需要任何处理

## 权限码

格式：`{MODULE}_{ACTION}`（如 `JOB_VIEW`、`JOB_END`、`DEPLOY_CREATE`、`DEPLOY_EXECUTE`、`DEPLOY_APPROVE`、`MONITOR_VIEW`、`USER_MANAGE`）。每个受保护端点必须有 `@PreAuthorize`。

## 事务与回滚（三层关系，2026-08-16 §19.2 更新）

> 新人容易把三者混为一谈——它们是**三个独立维度**，别混用。**§19.2 最新决策：本项目已取消全部 `@Transactional`（21 文件 77 处），为无事务架构。**

1. **无事务架构（`@Transactional` 零容忍，已全库统一 + 门禁）**
   - 主数据源 MySQL 8（InnoDB 单语句天然原子）；AS400/DB2 for i 仅只读（QSYS2 + 命令执行）。事务无收益，反而在 AS400 未开 Journaling 时引入 SQL7008 回滚静默失效风险。
   - **全库 0 处 `@Transactional`、0 处 `import org.springframework.transaction`**（2026-08-15），门禁 `bash scripts/check-transactional.sh`（**零容忍**：任何 `@Transactional` 含 rollbackFor 变体即失败，已接入 verify-all + backend CI）——新增任何 `@Transactional` 即失败。
2. **AS400 Journaling（`STRJRNPF IMAGES(*BOTH)`）= 能否回滚（AS400 基础设施，运维前置）**
   - DB2 for i 物理表默认不开启 Journaling；未开日志的表在事务里 COMMIT/ROLLBACK 会报 **SQL7008**，回滚静默失效。
   - **何时才需要开**：只有当 AS400/DB2 for i 成为本项目业务表的**写数据源**时才需要（当前主数据源是 MySQL 8，InnoDB 天然支持回滚；`JTOpenAS400Client` 只读 QSYS2 + 命令执行，不涉及事务）。若未来立项，按 §11.2 清单**全量 32 表**（28 核心 + 4 定时任务）一次开齐，不要只开「当前有写操作的表」——表会组合进同一事务（如 rx_doc + rx_doc_version），漏一张整个事务回滚失效；必须 `IMAGES(*BOTH)`（只记 *AFTER 只能审计不能回滚）。
3. **`app.tx.enabled` 动态开关 = 是否发事务指令（部署策略，当前未实现）**
   - 未实现的方案（Trae 审计 §12）：`false` 时注入 NoOpTransactionManager，doBegin/doCommit/doRollback 全空操作 → 不向 DB2 发 COMMIT/ROLLBACK → 表未开 Journaling 也不报 SQL7008。
   - ⚠️ 代价：数据直写不可回滚；「不报错」≠「数据安全」。仅限无 Journaling 的测试环境；生产必须 `true` + 开 Journaling。**优先方案是测试环境也开 Journaling，而不是关事务**。

> 结论：**当前无事务架构（§19.2），`@Transactional` 零容忍**；Journaling 与动态开关只在「AS400 主数据源」立项后才需要，届时再议（若恢复事务须同步调整门禁语义）。

## 分层准绳（代码审查清单，2026-08-15 落地）

> 新人写代码/评审前逐条自检，违反即打回（源自《可读性与新人上手》专项 Action 1）。

1. **Controller 不得注入 Mapper、不得 `new QueryWrapper`/`new LambdaQueryWrapper`**（2026-08-16 已全量清零：原唯一例外 I18nController 也已下沉 II18nService，白名单为空）。查询/分页逻辑一律下沉 Service。
   ```bash
   # 审查命令：应 0 命中（R1 白名单已清零，任何 Controller 直拼 Wrapper/注入 Mapper 即失败）
   grep -rn "new LambdaQueryWrapper\|new QueryWrapper" backend --include="*Controller.java"
   grep -rn "final .*Mapper " backend --include="*Controller.java"
   ```
2. **新增写接口必须用 Create/Update DTO 接收 `@RequestBody`**（禁止 Entity 当入参，防伪造 `id`/`createdBy`/`status` 等内部字段）。参照 `UserDTO/UserUpdateDTO`、`AlertRuleCreateDTO/AlertRuleUpdateDTO` 既有范例。
   ```bash
   # 审查命令：逐个核对 @RequestBody 是否为 DTO（而非 Entity）
   grep -rn "@RequestBody" backend --include="*Controller.java"
   ```
3. **新增返回接口优先复用既有 VO，禁止直接返 Entity**（system 模块 Admin-only CRUD 历史遗留属形状合规，见审查合订本轮次 10 遗留；新增代码必须走 VO）。实体内部字段（自增 id/审计字段/敏感列）不随 API 泄漏。
   ```bash
   # 审查命令：逐个核对 Controller 返回类型是否为 VO/基础类型（而非 Entity）
   grep -rn "ApiResponse<" backend --include="*Controller.java"
   ```
4. **Java 禁止内联全限定类名，一律顶部 import**（2026-08-24 全库清理 20 文件 40+ 处后立规）。`new com.xxx.Yyy(...)`、`com.xxx.Yyy::method`、`(com.xxx.Yyy.class)`、catch 子句 `catch (java.util.concurrent.RejectedExecutionException e)`、注解值 `value = org.springframework.http.HttpHeaders.AUTHORIZATION`、静态调用 `org.mockito.ArgumentMatchers.anyLong()` 等一律改为文件头 import + 简名引用。同包同名冲突等极少数必须用 FQN 时，加行注释说明原因。
   ```bash
   # 审查命令：项目类应 0 命中（排除 import/package/javadoc 行）
   rg -n "(new |=|\(|return|throw|catch|instanceof|,) *(com\.rxas400adm|java\.(util|time|regex)|org\.(springframework|mockito|junit|quartz))\.[A-Za-z0-9_.]+\.[A-Z]" backend -g "*.java" | rg -v "@link|@throws|@see"
   # 注意：小写静态方法调用（如 anyLong()）逃逸上面正则，需人工留意 verify(x, never()).y(anyLong()) 类写法
   ```

## 验证清单（每次改动后）

```bash
cd backend && mvn -q test && mvn -q -DskipTests package   # 后端：测试 + 打包
cd frontend && npm run build                               # 前端：类型检查 + 构建
bash scripts/verify-all.sh                                 # 一键门禁：分层准绳 + 插槽行类型 + CRLF 拼行 + V38 静态 + 迁移结构 + M1 全新库校验（SKIP_DB=1 跳过数据库项）
```

## 门禁清单（静态检查总览，2026-08-15 收口）

| 脚本 | 检查内容 | 白名单状态 | CI 接入 | 备注 |
| --- | --- | --- | --- | --- |
| `scripts/check-layering.sh` | 分层准绳：Controller 禁注入 Mapper / 禁 new QueryWrapper、写接口必须 DTO、返回禁直返 Entity | 存量遗留（例外页 + Admin-only CRUD，明细见脚本 ALLOW_* 清单） | backend.yml `Layering rules gate` | 新增违规即 exit 1；`--strict` 连遗留也计失败；支持传目标目录供回归测试 |
| `scripts/check-frontend-slots.sh` | R1：带作用域解构的 #default 插槽裸解构仅**警告**（el-table 窄标注会炸 vue-tsc，见插槽行类型收窄条目）；R2：标注 RowType 必须已 import 或本地声明，内联匿名对象类型合法 | —（R1 警告不阻断；R2 无遗留） | frontend.yml `Slot typing gate` | 支持传目标目录参数供回归测试；正则禁用 `$]` 字符类且 `\{` 后不能紧跟 `(`/`[`（见下方环境坑） |
| `frontend/scripts/check-template-join.mjs` | CRLF 批量替换吞换行回归：`el-*` 开标签与 `<template #default>` 同处一行即失败 | — | 并入 `npm run lint` | CRLF 批量替换必须用含换行的多行 oldString；检测函数已导出并有 vitest 回归测试 |
| `frontend/scripts/check-template-classes.mjs` | 模板自定义 class 必须有适用作用域样式定义：全局（common.css/theme.css + 未标 scoped 的 `<style>` 块 + `:deep()` 穿透类）／ 本组件 style 块 ／ 父组件 scoped 对子组件**根元素**豁免（Vue scoped 真实语义，组件树经 import+模板标签解析）；`:style="..."` 绑定屏蔽不提取（内联样式非 class）；白名单 el-*/fa-*/v-enter | **无**（73 .vue + 2 全局 CSS 全绿） | 并入 `npm run lint` + `verify-all` | 实测抓到 header 按钮样式丢失（父 scoped 到不了子组件内部）；新增样式必须按「作用域归属」放置：子组件要用的样式放全局块或该子组件自己的 style 块，禁止依赖父组件 scoped |
| `frontend/src/__tests__/gates.test.ts` | 门禁脚本自身的回归测试：插槽 R1/R2、分层 R1~R3、V38 一致性、迁移结构 fixture 驱动、拼行检测、模板 class scoped 判定/:style/子根豁免、i18n prefix.add、菜单页禁 default-expand-all（共 75 例） | — | frontend.yml `Unit tests (vitest)` | 防改脚本时破坏检测逻辑 |
| `frontend/scripts/check-i18n.mjs` | i18n key 一致性（$t 引用存在于 zh-CN、zh-CN/en-US key 集合一致）+ useFormDialog `i18nPrefix` 命名空间必须含 `add` 键（新增弹窗标题） | — | frontend.yml `i18n key check` | `npm run check:i18n`；walk 排除 `__tests__`/`*.test.ts`（测试 fixture 非源码） |
| `scripts/verify-fresh-db.sh` | M1 全新库单源一致性（V1~V42 迁移后断言结构计数 = V38 种子） | — | backend.yml `Verify fresh-DB M1 consistency` | 需 mysql 客户端 + 已打包 jar + 本地 MySQL；V38 种子变更须同步 V38_EXPECT_* |
| `scripts/check-v38-consistency.mjs` | V38 种子 ↔ V38_EXPECT_* 静态一致（解析 V38 SQL 算出行数，无需 MySQL） | — | backend.yml `V38 seed static consistency` | 静态快校验，V38 与期望值任一侧漂移即失败 |
| `scripts/check-migrations.mjs` | 迁移结构一致性：V{n}__desc.sql 命名/版本连续唯一、表/索引跨迁移去重、空迁移拦截、MANIFEST 文档化对象断言（V40/V41/V42） | 1 对已知遗留（V29/V30 重复建 rx_scheduler_lock，IF NOT EXISTS 幂等不可删） | backend.yml `Migration structure consistency` | 新增迁移必须按脚本头「固定模板」（命名/幂等/进 MANIFEST）；无需 MySQL；有 vitest 回归 |
| `scripts/check-transactional.sh` | 事务注解一致性：任何 `@Transactional` 必须显式 `rollbackFor = Exception.class`（裸注解/无 rollbackFor 变体即失败） | **无**（55 处已全量补齐，零遗留起步） | backend.yml `Transaction rollbackFor consistency` | 新增裸注解即失败；支持传目标目录供回归测试；无需 MySQL |
| `scripts/verify-all.sh` | 根级一键门禁：聚合上面分层/插槽/拼行/模板 class/i18n + V38 静态 + 迁移结构 + 事务注解 + M1 | — | 本地 / `.githooks/pre-commit` | `SKIP_DB=1` 跳过 M1 快速跑（八道静态门禁） |

**环境坑（务必读）**：① 本执行环境会把引号内 `$]` 展开成版本号（`[A-Za-z_$]` 会被破坏为正则语法错误）——脚本正则一律用 `[A-Za-z0-9_]` / perl `[\w]`；② CRLF 文件的批量文本替换必须用**含换行的多行 oldString**，否则吞掉 `<template>` 前的换行（lint 拼行防护可拦，24 处实测回归）；③ 本机 grep 3.0 对 `\{` 后紧跟 `(` 或 `[` 的交替/字符类匹配失效（`\{ (row|data)` 带空格正常）——交替拆多条固定 grep 或写成 `\{ (a|b)`。**三坑详解 + 自查清单见 `docs/RXAS400ADM-工具链正则避坑指南.md`。**

端到端冒烟（后端已启动）：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s http://localhost:8080/api/v1/auth/menu -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/as400/servers -H "Authorization: Bearer $TOKEN"
```

## Git 安全

- 提交前 `git status` + `git diff` 检查，**不要 `git add -A`**，只暂存本次改动相关文件
- 建议启用提交前门禁：`git config core.hooksPath .githooks`（提交自动跑八道静态门禁——分层/插槽/拼行/V38/迁移结构/事务注解/模板 class/i18n，失败即阻断；**再加后端 `mvn test` 阶段**，防止 API 改造漏改过时测试；紧急跳过 `SKIP_VERIFY_ALL=1 git commit`，仅跳过后端单测 `SKIP_BACKEND_TESTS=1 git commit`）
- PR 模板已就绪：`.github/PULL_REQUEST_TEMPLATE.md`（门禁自检清单 + 两道 review 结论，CI 红即不合，禁止合并）
- 不 push、不 rebase，除非用户明确要求
- 注意多代理环境：他人可能同时编辑/切换分支，先检查再动
