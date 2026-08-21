# 前端布局与样式优化方案

> **文档类型**: 分析报告 + 修复计划  
> **创建日期**: 2026-08-20  
> **状态**: ✅ 全部修复完成  
> **目标**: 修复布局/样式 Bug，统一设计规范，提升可访问性与响应式体验

---

## 一、Bug 级别修复（P0）

### 1.1 未定义的 CSS 变量 `--bg-card`

**位置**: [roles/index.vue:L353](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/system/roles/index.vue#L353)

**问题**: `.role-perms` 使用了 `background: var(--bg-card)`，但 `--bg-card` 在 `theme.css` 中未定义。浏览器回退为 `transparent`，暗色主题下右侧权限面板完全透明，文字无法阅读。

**修复**: 改为 `var(--bg-container)`。

### 1.2 硬编码颜色 `#fff`

**位置**: [calendar/index.vue:L280](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/calendar/index.vue#L280)

**问题**: `.today-dot` 使用 `color: #fff` 硬编码，违背项目"禁止散落硬编码色"的规范。

**修复**: 改为 `var(--bg-container)`，确保亮/暗主题下文字与主色背景均有足够对比度。

---

## 二、高优先级优化（P1）

### 2.1 零响应式设计 — 桌面端固定布局

**问题**: 项目中没有任何 `@media` 查询，所有页面使用固定宽度类（`w-90` ~ `w-320`）和 `el-col :span="N"`，在小屏幕（1366×768）下会挤压变形。

**受影响页面**:
- `Dashboard.vue` — `el-col :span="6"`（4 列），< 1200px 时卡片挤压
- `Monitor.vue` — `el-col :span="8"`（3 列图表），< 1100px 时图表无法正常显示
- `Source.vue` — `el-col :span="6"` + `el-col :span="18"` 左右分栏

**修复**: 在 `common.css` 中添加响应式断点，并对 Dashboard/Monitor 的 `el-col` 添加响应式 `span` 属性。

### 2.2 `tableMaxHeight` 自适应 vs 固定高度不一致

**问题**: 部分页面使用固定 `max-height` 像素值，大屏浪费空间，小屏可能溢出。

| 页面 | 当前方式 | 问题 |
|------|----------|------|
| `biz/data/index.vue` | `:max-height="600"` | 固定 600px |
| `Monitor.vue` | `max-height="360"` | 固定 360px |
| `job/index.vue` | `max-height="560"` | 固定 560px（日志抽屉） |

**修复**: 将 `biz/data` 和 `Monitor` 的表格改为 `page-container--fit` 弹性布局或使用 `enableResize: true` 动态计算高度。`job/index.vue` 的日志抽屉保持固定高度（抽屉内合理）。

---

## 三、中优先级优化（P2）

### 3.1 `search-bar` 底部间距不统一

**问题**: `common.css` 中 `.search-bar` 的 `margin-bottom: 12px`，但 Dashboard 手动添加了 `mb16`。说明 12px 在某些场景不够用。

**修复**: 统一 `.search-bar` 的 `margin-bottom` 为 16px，移除 Dashboard 中多余的 `mb16`。

### 3.2 `el-card` 阴影不一致

**问题**: 项目中混用 `shadow="never"`、`shadow="hover"`、默认 `shadow="always"`。项目已用 `border` + `border-radius` 区分卡片，阴影应统一。

**修复**: 统一为 `shadow="never"`（扁平风格），与 `--bg-container` + `border` 的卡片风格一致。

### 3.3 缺少键盘焦点指示器

**问题**: 高对比度主题注释提到"键盘用户可见的 3px 焦点环"，但实际未实现。自定义的 `header-action-btn`、`tag-item` 等没有 `:focus-visible` 样式。

**修复**: 在 `common.css` 中添加全局 `:focus-visible` 样式。

### 3.4 无骨架屏加载占位

**问题**: 所有页面只使用 `v-loading`，首次进入时整个区域空白 + 转圈。

**修复**: 创建 `RxSkeleton` 组件（支持 `table`/`card`/`list` 三种类型），并完成全量迁移——**所有 54 处 `v-loading` 已替换为骨架屏组件**。

**迁移覆盖 39 个文件**，包括：
- `Dashboard.vue`、`biz/data/index.vue`
- `job/index.vue`（3 个 tab）、`job/sla/index.vue`（2 个 table）、`job/dependency/index.vue`（chart）
- `system/roles/index.vue`（table + menu tree）、`system/Users.vue`（3 个 table）、`system/tasks/index.vue`
- `system/permissions/index.vue`、`system/permissionRequest/ApprovalPanel.vue`、`system/notifications/index.vue`、`system/notice/index.vue`
- `system/webhooks/index.vue`（2 个 table）、`system/config/index.vue`、`system/cache/index.vue`
- `system/i18n/index.vue`、`system/ipRules/index.vue`、`system/loginLog/index.vue`、`system/dict/index.vue`（2 个 table）、`system/menus/index.vue`
- `tool/region/index.vue`、`data/messageFiles/index.vue`、`data/sysvals/index.vue`、`data/tableFields/index.vue`
- `audit/index.vue`、`docs/index.vue`、`objects/index.vue`（4 个 table）、`ifs/index.vue`
- `schedule/index.vue`（2 个 table）、`executions/index.vue`、`scripts/index.vue`、`subsystems/index.vue`
- `topology/index.vue`（chart）、`health/index.vue`、`monitor/alertRules/index.vue`、`monitor/serverCompare/index.vue`（table + chart）
- `report/index.vue`（2 个 table）、`pf/index.vue`（2 个 table）、`assets/index.vue`

---

## 四、低优先级优化（P3）

### 4.1 缺少 `prefers-reduced-motion` 支持

**问题**: `flash-pop` 动画和 `fade-transform` 过渡没有考虑无障碍偏好。

**修复**: 在 `common.css` 中添加 `@media (prefers-reduced-motion: reduce)` 规则。

### 4.2 缺少打印样式

**问题**: B 端系统有打印报表/列表的需求，当前无 `@media print` 样式。

**修复**: 在 `common.css` 中添加基础打印样式，隐藏侧边栏、标签栏、操作按钮。

---

## 五、修复清单

| # | 优先级 | 问题 | 文件 | 状态 |
|---|--------|------|------|------|
| 1 | 🔴 P0 | `--bg-card` 未定义 | `roles/index.vue` | ✅ 已修复 |
| 2 | 🔴 P0 | `#fff` 硬编码 | `calendar/index.vue` / `Dashboard.vue` | ✅ 已修复 |
| 3 | 🟠 P1 | 零响应式设计 | `common.css` + Dashboard/Monitor/Source | ✅ 已修复 |
| 4 | 🟠 P1 | `tableMaxHeight` 不一致 | `biz/data/index.vue`、`Monitor.vue` | ✅ 已修复 |
| 5 | 🟡 P2 | `search-bar` 间距不统一 | `common.css` + `Dashboard.vue` | ✅ 已修复 |
| 6 | 🟡 P2 | `el-card` shadow 不一致 | `Dashboard.vue` | ✅ 已修复 |
| 7 | 🟡 P2 | 缺少键盘焦点指示器 | `common.css`（已存在，无需修改） | ✅ 已存在 |
| 8 | 🟡 P2 | 无骨架屏基础样式 | `common.css` + `RxSkeleton.vue` + 39 个页面 | ✅ 全量迁移完成（54 处 v-loading → RxSkeleton） |
| 9 | 🟢 P3 | `prefers-reduced-motion` | `common.css` | ✅ 已修复 |
| 10 | 🟢 P3 | 打印样式 | `common.css` | ✅ 已修复 |