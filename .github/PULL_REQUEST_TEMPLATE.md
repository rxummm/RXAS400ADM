# PR 描述

## 变更内容

<!-- 一句话说明本次改动解决什么问题（Why 优先，不是 What 清单） -->

- 

## 关联

<!-- 关联的 issue / code review 合订本轮次（如《CodeReview-RXAS400ADM-2026-08-14.md》追加 N）/ 文档 -->

- 

---

## 门禁自检清单（提交前逐项核对，CI 红即不合，禁止合并）

> 两道 review 结论（2026-08-15 落地）是硬性准绳，新增代码违反即打回：
> **分层准绳**（Action 1）：① Controller 禁注入 Mapper、禁 `new QueryWrapper`/`LambdaQueryWrapper`（只读小查询例外 I18nController.translations）；② 新增写接口必须 Create/Update DTO 接收 `@RequestBody`，禁 Entity 当入参；③ 新增返回接口优先复用 VO，禁直返 Entity。
> **插槽类型化约定**（Action 2）：表格/树/通用作用域插槽（`{ row }`/`{ data }`/`{ item }`/`{ node, data }`…）必须显式标注类型 `: { row: X }`，且 X 必须已 import 或本地声明；无作用域内容插槽（`#header`/`#footer`/`#empty`）不需要。

- [ ] **后端分层准绳**：`bash scripts/check-layering.sh`（新增文件零违规；存量遗留见脚本 `ALLOW_*` 白名单，新增不得进白名单）
- [ ] **前端插槽行类型**：`bash scripts/check-frontend-slots.sh`（全库零遗留，裸插槽 / 悬空 RowType 即失败）
- [ ] **CRLF 拼行防护**：`cd frontend && npm run lint`（eslint + `check-template-join.mjs`；CRLF 批量替换必须用含换行的多行 oldString）
- [ ] **i18n key 一致性**：`cd frontend && npm run check:i18n`（zh-CN/en-US key 集合一致，`$t` 引用存在）
- [ ] **V38 种子静态一致性**：`node scripts/check-v38-consistency.mjs`（改 V38 种子必须同步 `verify-fresh-db.sh` 的 `V38_EXPECT_*`）
- [ ] **迁移结构一致性**：`node scripts/check-migrations.mjs`（新增迁移：`V{n}__desc.sql` 版本连续唯一、表/索引跨迁移不重复、非空、文档化对象进 MANIFEST——模板见脚本头）
- [ ] **构建/测试**：`cd backend && mvn -q test`、`cd frontend && npm run build`
- [ ] **M1 全新库单源一致性**（涉及迁移/种子时必跑）：`bash scripts/verify-fresh-db.sh`（需本地 MySQL）
- [ ] 本地一键门禁：`SKIP_DB=1 bash scripts/verify-all.sh`

> 提交前钩子已就绪：`git config core.hooksPath .githooks`（提交自动跑静态门禁，失败即阻断；紧急跳过 `SKIP_VERIFY_ALL=1 git commit`）。

---

## 测试情况

<!-- 贴关键验证输出：门禁 exit 码、构建结果、测试数量 -->

- 

## 影响面 / 风险

<!-- 涉及的表/接口/页面；是否需要回滚；演示账号或生产数据影响 -->

-
