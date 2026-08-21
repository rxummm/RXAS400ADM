# 工具链正则避坑指南（2026-08-15 实测沉淀）

> 写给在本仓库（Git Bash / grep 3.0 / CRLF 文件）写脚本和批量改代码的人。
> 三个坑全部来自真实事故（合计 24 处 CRLF 拼行回归 + 1 个门禁静默空跑 + 1 个正则语法错误），
> 每坑附症状 / 复现 / 正确写法。对应门禁：`scripts/check-frontend-slots.sh`、
> `frontend/scripts/check-template-join.mjs`、`scripts/check-layering.sh`。

---

## 坑 1：引号内的 `$]` 会被展开成版本号

**症状**：正则里写 `[A-Za-z_$]` 这类含 `$` 的字符类，执行时报正则语法错误
（`Unmatched [ in regex`），且错误里 `$]` 变成了一个版本号（如 `5.042002`）——
引号（含单引号）挡不住，命令/脚本经过本执行环境时被预先展开。

**复现**：

```bash
grep -E '[A-Za-z_$]' file    # ❌ $] 被展开，正则损坏
```

**正确写法**：标识符类一律不用 `$`：

```bash
grep -oE '[A-Za-z0-9_]+'     # ✅ 覆盖字母数字下划线
perl -ne '/([\w]+)/'         # ✅ perl 用 \w
```

**影响**：所有 bash 脚本里的正则（`check-layering.sh` / `check-frontend-slots.sh` /
`check-template-join.mjs` 的 grep 调用）。注意 **node 脚本不受影响**（无 shell 展开）。

---

## 坑 2：CRLF 文件批量替换吞换行

**症状**：对 CRLF 行尾的 `.vue` 做**单行 oldString** 批量替换时，被替换标签**前面**的换行被吞掉，
`<el-table-column ...>` 与 `<template #default>` 拼到同一行（2026-08-15 实测 **24 处**回归，
全部被 lint 门禁 `vue/multiline-html-element-content-newline` 拦下）。

**复现**：

```text
oldString:  <template #default="{ row }">          ← 单行，无换行
结果:       <el-table-column ...>            <template ...>   ← 换行没了
```

**正确做法**：oldString 必须**含换行**（多行），把上一行的结尾一起带上：

```text
oldString:  <el-table-column prop="x" :label="y">\n  <template #default="{ row }">
newString:  <el-table-column prop="x" :label="y">\n  <template #default="{ row }: { row: X }">
```

**兜底**：`frontend/scripts/check-template-join.mjs`（已并入 `npm run lint`）——
任何 `el-*` 开标签与 `<template #default>` 同处一行即失败。

### 坑 2 实操流程：CRLF 批量改代码五步法

对 CRLF 的 `.vue` 做批量改造（补插槽标注、改标签、删整段）时，按下面顺序做，
可以做到「零拼行回归、一次过 lint」：

```bash
# ① 改造前：基线扫描，确认当前 0 拼行（先确认基线干净，改完才能对比）
cd frontend && node scripts/check-template-join.mjs   # 期望 0 匹配

# ② 批量替换：一律用【含换行的多行 oldString】（见下方 5 条范例），
#    单行 oldString 在 CRLF 文件上必然吞掉前一行的 \r\n

# ③ 改完立即：lint（拼行防护已并入 npm run lint），有 warning 当场定位到文件
cd frontend && npm run lint

# ④ 收尾：构建 + 门禁
cd frontend && npm run build
bash scripts/check-frontend-slots.sh                   # 插槽行类型 R1/R2

# ⑤ 万一还是拼了行（比如用工具批量替换的）：eslint --fix 拆行后，
#    必须人工恢复缩进（--fix 会把 <template> 顶到列 0），再跑一遍 lint 确认
```

**5 条可复制的多行 oldString 范例**（关键：oldString **从上一行的结尾换行开始**，
把要改的那一行**连同它前面的标签行**一起包住）：

```text
# 范例 1：补行插槽类型标注（el-table-column，最常用）
oldString:  <el-table-column prop="status" width="100">\n    <template #default="{ row }">
newString:  <el-table-column prop="status" width="100">\n    <template #default="{ row }: { row: JobInfo }">

# 范例 2：补树插槽类型标注（el-tree）
oldString:  <el-tree :data="nodes" node-key="id">\n    <template #default="{ data }">
newString:  <el-tree :data="nodes" node-key="id">\n    <template #default="{ data }: { data: SysMenu }">

# 范例 3：补通用作用域插槽标注（el-select / el-tabs 等）
oldString:  <el-select v-model="val" filterable>\n    <template #default="{ item }">
newString:  <el-select v-model="val" filterable>\n    <template #default="{ item }: { item: OptionItem }">

# 范例 4：插入新列/新行（改前一行 + 插入行一起包住，避免只插单行）
oldString:    <el-table-column prop="status" width="100" />\n    <el-table-column prop="createdAt" width="180" />
newString:    <el-table-column prop="status" width="100" />\n    <el-table-column prop="remark" min-width="200" />\n    <el-table-column prop="createdAt" width="180" />

# 范例 5：删除整段（oldString 首尾各带一行邻居，把 \r\n 边界一起删掉）
oldString:    <template #footer>\n      <el-button @click="dialogVisible = false">取消</el-button>\n    </template>\n  </el-dialog>
newString:  </el-dialog>
```

> 单行 oldString 是否一定出事？dict/roles 用了多行 oldString 安然无恙，
> 而 9 个文件 24 处单行替换全部拼行——**CRLF 文件上不要赌单行**，一律多行。

---

## 坑 3：grep 3.0 对 `\{` 后紧跟 `(` 或 `[` 匹配失效（静默）

**症状**：`grep -E '\{(row|data) \}'` 或 `'\{[A-Za-z0-9_]+'` **匹配不到任何东西**，且**不报错**——
最危险：门禁静默空跑（实测 `check-frontend-slots.sh` R2 的文件发现因此失效，空跑了一轮才被测试暴露）。

**复现**：

```bash
grep -rlE '\{(row|data) \}: \{(row|data): '   # ❌ 0 匹配（本应命中 40 个文件）
grep -rlE '\{ row \}: \{ row: '              # ✅ 38 个文件，正常
grep -E  '\{ (row|data) \}'                  # ✅ 带空格，正常
```

**正确写法**：交替拆成多条**固定** grep 再 `sort -u`，或 `\{` 后**带空格**：

```bash
( grep -rlE '\{ row \}: \{ row: ' . ; grep -rlE '\{ data \}: \{ data: ' . ) | sort -u   # ✅
sed -nE 's/.*#default="\{ (row|data) \}: \{ (row|data): (.*) \}".*/\3/p'                # ✅ 带空格
```

**影响**：本机 grep 3.0（Git Bash 内置）；CI（ubuntu GNU grep）行为可能不同——因此**脚本必须按「保守写法」编写**，两边都要能过。

---

## 附加观察：内联命令的捕获组引用 `\1` 偶发损坏

在**终端内联**执行的 sed 里，`(.*)` 捕获 + `\1` 引用会偶发报 `invalid reference \1`（捕获组被环境剥掉）；
但**脚本文件内**（write_file 落盘后 `bash script.sh`）和 **vitest spawn** 中完全正常。
结论：写进脚本文件 + 跑测试验证，不要依赖内联 sed 调试正则。

---

## 自查清单（改动脚本后必跑）

```bash
bash scripts/check-layering.sh && bash scripts/check-layering.sh --strict     # 分层准绳
bash scripts/check-frontend-slots.sh                                           # 插槽行类型（R1/R2）
cd frontend && npm run lint                                                    # eslint + CRLF 拼行防护
cd frontend && npm test                                                        # 门禁回归 60 例（gates.test.ts）
node scripts/check-v38-consistency.mjs                                         # V38 种子静态一致性
node scripts/check-migrations.mjs                                              # 迁移结构一致性（V1~V42+）
SKIP_DB=1 bash scripts/verify-all.sh                                           # 一键聚合（五道静态 + 可选 M1）
```

---

## 关联

- 门禁总览（每脚本的规则/白名单/CI 接入）：见 `AGENTS.md`「门禁清单（静态检查总览）」。
- 事故记录：见《CodeReview-RXAS400ADM-2026-08-14.md》轮次 13 追加 4/6/7（CRLF 拼行、R2 空跑、grep 交替坑）。
