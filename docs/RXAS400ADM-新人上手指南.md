# RXAS400ADM 新人上手指南（30 天降本清单）

> 本文档由《CodeReview-RXAS400ADM-可读性与新人上手.md》（2026-08-15 审查）整理而来。
> 原文档的**问题/优化部分**（Top 5 核心坏味道、分级问题清单、原晦涩代码 vs 易读重构对比、验证基线、亮点保留）
> 已并入合订本 **《CodeReview-RXAS400ADM-2026-08-14.md》（第三部分）**；此处保留**完整的新人上手部分**——「30 天新人降本清单」。
>
> 审查目的（沿自原文档）：解决新人「看不懂逻辑、不敢改代码、找不到调用链」的核心痛点。
> 审查范围：`backend/`（Spring Boot 3.3 + MyBatis-Plus，多模块）、`frontend/`（Vue 3 + TS + Vite + Pinia + Element Plus）。
> 审查方法：全量静态扫描 + 逐文件人工核对（前端 144 源文件约 1.97 万行 / 后端 46 个 Controller、37 个 Entity、13 个 DTO、15 个 VO）。

---

## 30 天新人降本清单（前 3 个 Action Items，按性价比排序）

### Action 1 ⭐ 建立「三层准绳」与文档纠偏（约 1-2 天，收益最高）

**现状**：AGENTS.md 声称的 `PageParam` 不存在；system 模块 Controller 直接返 Entity；Controller 直接拼 Wrapper——新人按文档/惯例写出来的代码与存量代码冲突，第一周就"不敢改"。

**动作**：
1. 修正 AGENTS.md：删除虚构的 PageParam 约定，改为如实描述现状（"分页用散装 `@RequestParam`，边界校验待统一"）；
2. 起草并落地 3 条硬性分层准绳（进 lint/审查清单）：
   - Controller 不得注入 Mapper，不得 `new QueryWrapper`（允许的例外：只读小查询的 `I18nController.translations`）；
   - 新增写接口必须用 Create/Update DTO（参照 `UserDTO/UserUpdateDTO`、`AlertRuleCreateDTO/UpdateDTO` 的既有范例）；
   - 新增返回接口优先复用既有 VO，禁止再直接返 Entity（`Doc`/`SysRole` 等先从分页/列表接口改起）。

**新人收益**：第一周就能照着准绳写出与存量风格一致的代码，不用猜"这里为什么不加 Service"。

> **实施状态（2026-08-15 轮次 12/13）**：文档纠偏已完成——AGENTS.md 删除虚构的「Query 继承 PageParam」约定，改为「分页参数用 `@RequestParam current/size`，边界统一走 `PageConstants.clampNum/clampSize`（MAX_PAGE_SIZE=100）」，分页边界常量 `PageConstants` 已落地并全库替换。**「三层准绳」已于轮次 13 落地为 AGENTS.md「分层准绳（代码审查清单）」**——三条硬性规则（Controller 禁注入 Mapper/禁 new QueryWrapper、写接口必须 DTO、返回禁直返 Entity）各附可执行 grep 审查命令，提交前逐条自检。

### Action 2 ⭐ 消灭模板行 any：一个工具函数 + 两个最大表格页示范（约 3-5 天）

**现状**：144 处 `#default="{ row }"` 全是 any，改字段名零校验。

**动作**：
1. 写一个 `useTypedTable`（或约定文档）：凡表格行需声明 `RowType` 并在插槽 `#default="{ row }: { row: RowType }"` 显式标注；
2. 先改两个高改动频率的页面做示范：`system/Users.vue`（`UserVO.status` 顺手补 `'ACTIVE'|'DISABLED'` 联合类型）与 `job/index.vue`（同时顺手收敛 `'RUN'/'ACTIVE'` 状态魔法值）；
3. 在 AGENTS.md 前端规范补一条："el-table 插槽必须显式标注行类型，禁止裸 `{ row }`"。

**新人收益**：改表头/字段有编译期兜底，从"靠眼睛"变成"靠编译器"，犯错成本大幅下降。

> **实施状态（2026-08-15 轮次 13）**：已按「约定文档」路线落地（未引入无实质逻辑的 useTypedTable composable）——AGENTS.md 前端规范新增「el-table 插槽必须显式标注行类型，禁止裸 `{ row }`」；**两个示范页已改造完成**：`system/Users.vue`（4 处用户表插槽 + 2 处安全页插槽标注 `UserVO`/`LoginAttemptRecord`，`UserVO.status` 补 `'ACTIVE'|'DISABLED'` 联合类型）与 `job/index.vue`（活动作业/队列/SPOOL/日志/MSGW 共 6 处插槽标注 `JobInfo`/`JobQueueInfo`/`SpoolFile`/`JobLogRow`/`MsgwMessage`）。**顺带修正 6 处 API 类型契约与后端不符**（`api/job.ts` 的 JobInfo/JobQueueInfo/SpoolFile/fetchJobLog、`api/user.ts` 的 LoginAttemptRecord/IpStat 均按后端 VO/大写列名重写）——这正是插槽类型化暴露出的真实契约错误。

> **后续进度（2026-08-15 追加 1~6）**：批量标注持续推进——追加 1 清掉 18 个高频表格页 81 处，追加 2 清掉 system 存量 CRUD 批 11 文件 30 处（含 4 处 API 契约修正），追加 3 清掉 data 字典批 12 处，追加 4 收尾 docs/tool/ShortcutsHelp 9 处，追加 5 把约定扩展到 el-tree `{ data }` 树插槽（3 处），追加 6 把门禁泛化为「任何带作用域解构的 #default 插槽必须显式标注类型」并为门禁补 vitest 回归测试（19 例，`gates.test.ts`）。**全库 147 处 #default 插槽已 100% 显式标注，零裸解构**；进度清单见合订本「轮次 13 追加」C~H 节。
> **门禁已就位**：`scripts/check-frontend-slots.sh`（裸插槽 / **悬空 RowType 即失败**，已扩展覆盖树插槽，接入 frontend CI）+ `frontend/scripts/check-template-join.mjs`（CRLF 批量替换吞换行回归防护，并入 `npm run lint`）+ `scripts/verify-all.sh`（根级一键门禁，`SKIP_DB=1` 跳过数据库项）+ `.githooks/pre-commit`（提交前自动跑三道静态门禁，`git config core.hooksPath .githooks` 启用）。

### Action 3 ⭐ 统一分页与状态常量（约 2-3 天，顺手消除重复样板）

**现状**：分页上限 `Math.min(100, Math.max(1, size))` 复制 10 处且 `SysUserServiceImpl` 裸传无上限；前端 6-7 个页面手写分页样板。

**动作**：
1. 后端：在 `PageParam`（本次新建，落实文档承诺）或 `common` 常量类定义 `MAX_PAGE_SIZE = 100`、`DEFAULT_PAGE_SIZE = 10`，10 处复制点统一替换；`SysUserServiceImpl.java:60`、`RoleService.java:53` 补边界；
2. 前端：把 `assets/index.vue`（手写分页 + 手写弹窗）按 `system/permissions/index.vue` 的范式改造成 `useTablePage` + `useFormDialog`，作为"抽 hook 参照页"；
3. 顺带把 `executions/index.vue`、`ifs/index.vue` 的 Blob 下载换成现成 `triggerBlobDownload()`。

**新人收益**：分页写法全库统一，一个 composable 搞定，新增列表页只需 3 行。

> **实施状态（2026-08-15 轮次 12）**：① 后端已完成——走「或」选项的 `common` 常量类路线（`PageConstants`：MAX_PAGE_SIZE=100 / DEFAULT_PAGE_SIZE=10 / `clampSize` / `clampNum`），10+ 处复制点全量替换，`SysUserServiceImpl` 与 `RoleService`（核对中补上的遗漏项）裸传均已补边界；② 前端 Blob 下载已统一复用 `triggerBlobDownload()`（executions/ifs）；③ `assets/index.vue` 手写分页样板仍未切 `useTablePage`，待排期。

---

## 新人速查：详细问题与示例在哪看

- **Top 5 核心坏味道**（分层纪律失衡 / Controller 拼 Wrapper / 模板行 any / 敏感字段防护三种做法 / 巨石组件 setup）：见合订本《CodeReview-RXAS400ADM-2026-08-14.md》**第三部分 §一**；
- **分级问题清单**（Blocker / Warning / Info 全表）：见合订本**第三部分 §二**；
- **原晦涩代码 vs 易读重构代码 5 组对比**（含已修复项的现状标注）：见合订本**第三部分 §三**；
- **验证基线 / 亮点保留**（避免"为了重构而重构"）：见合订本**第三部分 §五 / §六**；
- **各项实施进度**（P0~P3 / N1~N6 / M1~M3 / 可读性专项轮次 12）：见合订本**第二部分 §9 起**与**第四部分**。
