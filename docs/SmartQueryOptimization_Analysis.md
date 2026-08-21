# 智能防抖过滤 + 强行穿透后端 — 通用数据表格查询逻辑优化方案

> **文档类型**: 现状分析 + 设计建议 + 实施跟踪  
> **创建日期**: 2026-08-20  
> **修订日期**: 2026-08-20（v4：Phase 1/2/3/4 全部完成 + 23 个页面已迁移至 useSmartQueryTable + 多 Tab 页面排查无遗漏）  
> **状态**: ✅ Phase 1/2/3/4 全部完成  
> **目标**: 在保留现有"输入框 + 查询按钮 + 刷新按钮"布局的前提下，封装一套高复用的【智能防抖过滤 + 强制后端查询】混合查询 Hook

---

## 目录

1. [当前项目页面全景分析](#1-当前项目页面全景分析)
2. [现有核心组件与 Composable 分析](#2-现有核心组件与-composable-分析)
3. [分页面模式分类与问题清单](#3-分页面模式分类与问题清单)
4. [与目标需求的差距分析](#4-与目标需求的差距分析)
5. [优化方案设计建议](#5-优化方案设计建议)
6. [大数量分页 vs 前端全量小数据的兼容策略](#6-大数量分页-vs-前端全量小数据的兼容策略)
7. [实施路线图建议](#7-实施路线图建议)

---

## 1. 当前项目页面全景分析

### 1.1 项目技术栈

| 层 | 技术 |
|---|---|
| 框架 | Vue 3.5 + TypeScript + Composition API |
| UI 库 | Element Plus 2.8 |
| 状态管理 | Pinia 2.2 |
| 路由 | Vue Router 4.4 |
| 构建 | Vite 5.4 |

### 1.2 页面总数

项目共有 **45 个 Vue 页面/组件** 涉及查询/搜索/刷新交互，其中：

- **19 个页面**使用 `useTablePage` composable（统一模式）
- **6 个页面**使用自定义手动查询逻辑（模式不统一）
- **其余页面**为 Dashboard / Monitor / 特殊交互页面

---

## 2. 现有核心组件与 Composable 分析

### 2.1 `QueryBar.vue` — 查询栏组件

**文件位置**: [QueryBar.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/components/QueryBar.vue)

**当前功能**:
- 关键字输入框（`v-model:keyword`），支持 `@keyup.enter` 触发搜索
- 查询按钮（Search 图标），点击触发 `emit('search')`
- 刷新按钮（Refresh 图标），点击触发 `emit('refresh')`
- 缓存来源提示 Tag（`fromCache` / `flashTick` 闪烁）
- 支持 `left` 默认插槽（筛选控件）和 `right` 插槽（操作按钮）

**当前行为**:
```
用户输入关键字 → 按回车或点击「查询」 → emit('search')
用户点击「刷新」 → emit('refresh')
```

**缺失的能力**:
- ❌ 无输入防抖（Debounce）机制
- ❌ 无自动前端过滤（依赖父组件 computed 响应式过滤，每次按键立即触发）
- ❌ 刷新按钮和查询按钮职责分离，但需求要求"刷新 = 携带当前条件强刷后端"（语义上已接近，但需确认同源）

### 2.2 `useTablePage.ts` — 核心表格 Composable

**文件位置**: [useTablePage.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useTablePage.ts)

**当前能力矩阵**:

| 能力 | 状态 | 说明 |
|---|---|---|
| `records` | ✅ | 原始数据（shallowRef） |
| `filteredData` | ✅ | computed：基于 keyword 的 searchFields 多字段模糊匹配 |
| `pagedData` | ✅ | computed：前端分页切片（frontendPage=true） |
| `loading` | ✅ | 请求加载状态 |
| `keyword` | ✅ | 双向绑定关键字 |
| `enableCache` | ✅ | 3 分钟内存缓存（按 route+params+serverId 隔离） |
| `isFromCache` | ✅ | 是否命中缓存 |
| `dataSourceTick` | ✅ | 数据源变更信号（配合 QueryBar 闪烁） |
| `handleSearch()` | ✅ | 重置页码 + fetchData（缓存有效则不发请求） |
| `handleRefresh()` | ✅ | 强制回后端（跳过缓存） |
| `handlePageChange()` | ✅ | 翻页 |
| `handleSizeChange()` | ✅ | 每页条数变更 |
| `resetSearch()` | ✅ | 清空 keyword + 重置页码 + fetchData |
| `fetchData()` | ✅ | 核心请求方法（支持 extraParams + forceRefresh） |
| `searchFields` | ✅ | 多字段模糊匹配配置 |
| `matchRow` | ✅ | 自定义行匹配函数 |
| `frontendPage` | ✅ | 前端分页模式 |
| `buildParams` | ✅ | 自定义请求参数组装 |
| `autoFetch` | ✅ | 挂载自动请求 |
| `updateItem/removeItem/addItem` | ✅ | 乐观更新 |
| `filterMode` | ✅ | 过滤模式（`all` / `any`） |
| `columnSettings` | ✅ | 列设置（localStorage 持久化） |

**当前 `filteredData` 行为**（关键代码）:
```typescript
// 位于 useTablePage.ts 第 145-157 行
const filteredData = computed<T[]>(() => {
  if (!keyword.value.trim()) return records.value
  const kw = keyword.value.trim().toLowerCase()
  if (matchRow) {
    return records.value.filter((item: T) => matchRow(item, kw))
  }
  if (searchFields.length === 0) return records.value
  return records.value.filter((item) =>
    searchFields.some((field) => {
      const v = (item as Record<string, unknown>)[field]
      return v != null && String(v).toLowerCase().includes(kw)
    }),
  )
})
```

**问题**:
- ⚠️ `filteredData` 是 **computed**，每次 keyword 变化立即触发。虽然对前端内存数据来说性能 OK，但缺少"用户停止输入后才触发"的防抖体验。
- ⚠️ 没有"前端过滤为空"时的引导性提示（如："未找到本地数据，可点击查询深度检索"）。
- ⚠️ `handleSearch()` 在 `enableCache=true` 时，如果缓存命中，**不发后端请求**。需求要求"点击查询/回车"必须强制走后端。

### 2.3 `debounce.ts` — 防抖工具函数

**文件位置**: [debounce.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/utils/debounce.ts)

```typescript
export function debounce<A extends unknown[]>(fn: (...args: A) => void, delay = 300) {
  let timer: ReturnType<typeof setTimeout> | undefined
  return (...args: A) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}
```

**使用情况**: 仅在 `data/tableFields/index.vue` 和 `biz/data/index.vue` 中用于 `<select>` 的 `@change` 事件，**未在任何页面的关键字输入框中使用**。

**缺失的能力**:
- ❌ 不支持 `cancel()` 方法（无法从外部清除定时器）
- ❌ 不支持 `flush()` 立即执行

### 2.4 `useFlash.ts` — 闪烁提示 Composable

**文件位置**: [useFlash.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useFlash.ts)

用于数据来源提示的闪烁动画（QueryBar 缓存命中/刷新后），功能完整，无需修改。

---

## 3. 分页面模式分类与问题清单

### 3.1 模式 A：使用 `useTablePage` + `QueryBar`（19 个页面）

| 页面 | 使用特性 | 备注 |
|---|---|---|
| `job/index.vue` | frontendPage, enableCache, searchFields | 活动作业，全量前端过滤 |
| `system/roles/index.vue` | 服务端分页, columnSettings | 角色管理，无前端过滤 |
| `audit/index.vue` | 服务端分页, enableCache, searchFields, filters | 审计日志，混合模式 |
| `objects/index.vue` | enableCache, buildParams, filters | 对象搜索 |
| `ifs/index.vue` | frontendPage, enableCache, searchFields | IFS 文件浏览 |
| `schedule/index.vue` | frontendPage, enableCache, searchFields | 定时任务 |
| `data/sysvals/index.vue` | frontendPage, enableCache, searchFields, filterMode | 系统值 |
| `system/i18n/index.vue` | 服务端分页, lang filter | 国际化管理 |
| `system/ipRules/index.vue` | 服务端分页, type filter | IP 规则 |
| `system/loginLog/index.vue` | 服务端分页, username/action filters | 登录日志 |
| `system/notice/index.vue` | 服务端分页, status filter | 公告管理 |
| `system/permissions/index.vue` | 服务端分页, module filter | 权限码管理 |
| `data/messageFiles/index.vue` | 服务端分页 | 消息文件 |
| `docs/index.vue` | frontendPage, enableCache | 文档管理 |
| `subsystems/index.vue` | 服务端分页 | 子系统 |
| `scripts/index.vue` | 服务端分页 | 脚本管理 |
| `executions/index.vue` | 服务端分页 | 执行记录 |
| `biz/data/index.vue` | 服务端分页, debounce on select | 业务数据 |
| `pf/index.vue` | 服务端分页 | 性能数据 |

**共性**:
- 都使用 `QueryBar` 组件（统一 UI）
- 都通过 `useTablePage` 管理数据状态
- `handleSearch` 和 `handleRefresh` 是两个独立事件

### 3.2 模式 B：自定义手动查询（6 个页面）

| 页面 | 查询方式 | 问题 |
|---|---|---|
| `system/Users.vue` | 手动 `search-bar` + `load()` + `keyword` ref | 无 useTablePage，无缓存，无前端过滤 |
| `system/cache/index.vue` | 手动 `search-bar` + `load()` + computed `rows` | 前端过滤但不走 useTablePage |
| `system/tasks/index.vue` | 手动 `search-bar` + `load()` + computed `rows` | 同上 |
| `system/config/index.vue` | 手动 `search-bar` + `load()` + computed `rows` | 同上 |
| `system/webhooks/index.vue` | 手动 `search-bar`（logs tab） | 部分使用 useFormDialog |
| `system/dict/index.vue` | 手动 `search-bar` + computed `filteredTypes` | 字典类型左栏过滤 |

**问题**:
- 代码重复：每个页面自己实现 `keyword` ref + `computed` 过滤 + `load()` 函数
- 无缓存机制
- 无防抖
- 无统一 Loading 状态管理
- 刷新逻辑不统一（有的有刷新按钮，有的没有）

### 3.3 模式 C：特殊交互页面

| 页面 | 查询方式 |
|---|---|
| `query/index.vue` | SQL 编辑器 + 执行按钮（非表格查询模式） |
| `report/index.vue` | 表单 + 下载按钮（非表格查询模式） |
| `Monitor.vue` | WebSocket 实时推送（非查询模式） |
| `Dashboard.vue` | 组件化仪表盘 |

---

## 4. 与目标需求的差距分析

### 4.1 需求 1：初始化状态

> 页面首次加载，输入框为空，用户点击查询或输入条件后点击查询请求后端 API 获取初始数据集（originData），赋值给 tableData。

**当前状态**: ✅ 已实现  
`useTablePage` 的 `autoFetch: true`（默认）在 `onMounted` 时自动调用 `fetchData()`。  
用户点击查询触发 `handleSearch()` → `fetchData()`。

**结论**: 无需修改。

### 4.2 需求 2：输入停顿自动过滤（Debounce 500ms）

> 用户停止输入满 500ms 后，系统自动在 originData 内存中进行多字段模糊匹配，无需点击查询按钮。  
> 边界情况：前端匹配为空时，显示"未找到本地数据，可点击查询深度检索"。

**当前状态**: ❌ 未实现

| 差距项 | 当前状态 |
|---|---|
| 防抖 500ms | ❌ `filteredData` 是 computed，每次 keydown 立即触发 |
| 自动前端过滤 | ⚠️ 已有 `filteredData` 逻辑，但缺少防抖包装 |
| 空结果引导提示 | ❌ 表格空态只显示通用 `$t('common.noData')`，无引导语 |
| 独立的 `originData` 概念 | ⚠️ `records` 等价于 `originData`，但语义不够明确 |

**结论**: 需要新增防抖层 + 空结果引导提示组件。

### 4.3 需求 3：点击查询按钮 / 回车 → 强制后端查询

> 点击查询按钮或在输入框敲回车时，必须立刻打断/清除正在进行的输入防抖事件，立刻强制向后端发起 API 请求。  
> 表格展示 Loading 动画，用当前输入条件向服务器请求最新数据，回显并更新 originData 与 tableData。

**当前状态**: ❌ 部分不满足

| 差距项 | 当前状态 |
|---|---|
| 打断防抖定时器 | ❌ 无防抖，自然无打断 |
| 强制后端请求 | ⚠️ `handleSearch()` 在 `enableCache=true` 时如果缓存命中，不发后端请求 |
| Loading 动画 | ✅ `loading` ref 已绑定到 `v-loading` |
| 回车触发 | ✅ `QueryBar` 有 `@keyup.enter="emit('search')"` |

**关键问题**: 当前的 `handleSearch()` 逻辑是"缓存有效则不发请求"，但需求要求查询按钮必须强制走后端。需要将"查询"和"刷新"的语义合并：
- 查询按钮 = 强制后端（当前 `handleRefresh` 的行为）
- 输入防抖 = 前端自动过滤（新能力）

**结论**: 需要重构 `handleSearch` 语义，使其等同于当前的 `handleRefresh`（forceRefresh=true）。

### 4.4 需求 4：右上角刷新图标 → 同源触发

> 在表格右上方保留一个纯图标的旋转箭头（↻）作为刷新手段。  
> 点击该图标时，与点击查询按钮完全同源——复用同一个请求函数，携带当前相同的输入条件强刷后端。

**当前状态**: ⚠️ 部分满足

`QueryBar` 的刷新按钮触发 `emit('refresh')`，页面调用 `handleRefresh()` → `fetchData({}, true)`。  
查询按钮触发 `emit('search')`，页面调用 `handleSearch()` → `fetchData()`。

**差距**: 当前两个事件走不同路径——`handleSearch` 可能走缓存，`handleRefresh` 强制后端。但需求要求两者同源（都强制后端），因此需要统一。

**结论**: 查询按钮和刷新图标统一触发同一个 `forceSearch` 方法（等同于当前的 `handleRefresh`）。

---

## 5. 优化方案设计建议

### 5.1 核心设计思路

创建一个新的 Composable：**`useSmartQueryTable`**，在 `useTablePage` 基础上增强以下能力：

```
┌─────────────────────────────────────────────────────┐
│                  useSmartQueryTable                   │
│                                                       │
│  ┌──────────┐    ┌──────────────┐    ┌────────────┐ │
│  │ keyword   │───▶│ Debounce     │───▶│ frontend   │ │
│  │ (v-model) │    │ Timer 500ms  │    │ filter     │ │
│  └──────────┘    └──────────────┘    └────────────┘ │
│       │                  │                  │         │
│       │ 回车/点击查询     │                  │         │
│       │ 点击刷新图标      │                  │         │
│       ▼                  ▼                  ▼         │
│  ┌──────────────────────────────────────────────┐   │
│  │           cancel() + forceSearch()            │   │
│  │           → 更新 originData + tableData       │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### 5.2 文件结构

```
src/composables/
  ├── useTablePage.ts          # 保留（标记 @deprecated），迁移完成后移除
  ├── useSmartQueryTable.ts    # 新增，完全替代 useTablePage
  └── useFlash.ts              # 保留不变

src/utils/
  ├── debounce.ts              # 保留不变（用于其他场景）
  └── cancellableDebounce.ts   # 新增（可取消防抖）

src/components/
  └── QueryBar.vue             # 改造：统一查询/刷新为 forceSearch 事件 + 空结果引导
```

**不需要 `SmartQueryBar.vue`**：防抖逻辑属于 Composable 层（`useSmartQueryTable`），UI 组件只负责 emit 事件，不需要额外组件。

### 5.3 `useSmartQueryTable` 接口设计（草案）

```typescript
export function useSmartQueryTable<T>(options: SmartQueryTableOptions<T>) {
  return {
    // ===== 数据 =====
    originData,       // 后端返回的原始全量数据
    tableData,        // 表格实际展示数据（前端过滤后或后端分页后）
    loading,          // 加载状态
    isFromCache,      // 是否来自缓存
    dataSourceTick,   // 数据源变更信号（配合 QueryBar 闪烁）

    // ===== 分页 =====
    current,          // 当前页码
    size,             // 每页条数
    total,            // 总条数
    pageSizes,        // 每页条数选项

    // ===== 关键字 =====
    keyword,          // 双向绑定关键字（v-model）
    localFilterEmpty, // 是否因前端过滤导致空结果
    localFilterHint,  // 空结果引导提示文案（computed）

    // ===== 操作方法 =====
    forceSearch,      // 强制后端查询（查询按钮/回车/刷新图标 同源）
    handlePageChange, // 翻页
    handleSizeChange, // 每页条数变更
    resetSearch,      // 重置搜索（清空 keyword + 重置页码 + forceSearch）

    // ===== 乐观更新（继承自 useTablePage）=====
    updateItem,       // 更新单行数据
    removeItem,       // 删除单行数据
    addItem,          // 新增单行数据

    // ===== 列设置（继承自 useTablePage）=====
    columnSettings,   // 列设置对象
  }
}
```

### 5.4 核心状态流转图

```
                    ┌──────────────────────┐
                    │    页面首次加载        │
                    │  autoFetch / 点击查询  │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   fetchData()         │
                    │   → originData        │
                    │   → tableData         │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
     ┌────────────┐   ┌──────────────┐   ┌──────────────┐
     │ 用户输入    │   │ 点击查询/回车 │   │ 点击刷新图标  │
     │ (keyword   │   │              │   │              │
     │  变化)     │   │              │   │              │
     └─────┬──────┘   └──────┬───────┘   └──────┬───────┘
           │                 │                   │
           ▼                 │                   │
     ┌──────────────┐        │                   │
     │ 启动/重置     │        │                   │
     │ 防抖定时器    │        │                   │
     │ (500ms)      │        │                   │
     └──────┬───────┘        │                   │
            │                │                   │
            ▼                ▼                   ▼
     ┌──────────────┐  ┌────────────────────────────────┐
     │ 500ms 后触发  │  │   cancel() 清除防抖定时器       │
     │ 前端过滤      │  │   + forceSearch() 后端请求       │
     │ originData   │  │   → loading=true               │
     │ → tableData  │  │   → API 请求                    │
     │              │  │   → 更新 originData + tableData │
     │ 空结果?       │  │   → loading=false              │
     │ 显示引导提示  │  └────────────────────────────────┘
     └──────────────┘
```

### 5.5 可取消防抖机制

#### 5.5.1 为什么需要"可取消"？

**核心场景：防止"前端过滤"和"后端请求"发生竞态覆盖。**

```
时间轴 ──────────────────────────────────────────────────▶

用户输入 "jo"     用户点击【查询】按钮
    │                  │
    ▼                  ▼
启动防抖定时器     期望：立刻走后端
(500ms 后触发        BUT：定时器还在跑！
 前端过滤)          
    │                  
    │    ┌──────────────────────────────────────┐
    │    │ 如果不取消定时器（现有 debounce.ts）：  │
    │    │                                      │
    │    │ ① 后端请求发出                        │
    │    │ ② 后端返回新数据                      │
    │    │ ③ tableData = 后端新数据              │ ← 用户看到正确结果
    │    │ ④ 500ms 定时器到期                    │
    │    │ ⑤ tableData = 前端过滤(旧数据)         │ ← 数据被覆盖！Bug!
    │    └──────────────────────────────────────┘
    │
    ▼
必须 cancel() 清掉定时器 → ④ ⑤ 不会发生
```

#### 5.5.2 实现代码

```typescript
// src/utils/cancellableDebounce.ts
// 与现有 debounce.ts 互补，不替换

export function createCancellableDebounce(delay = 500) {
  let timer: ReturnType<typeof setTimeout> | undefined

  const schedule = (fn: () => void) => {
    cancel()  // 先清除之前的定时器，确保只有最后一次输入有效
    timer = setTimeout(fn, delay)
  }

  const cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = undefined
    }
  }

  // 立即执行，跳过等待
  const flush = (fn: () => void) => {
    cancel()
    fn()
  }

  return { schedule, cancel, flush }
}
```

#### 5.5.3 在 useSmartQueryTable 中的使用方式

```typescript
const { schedule, cancel } = createCancellableDebounce(500)

// 用户输入 → 启动防抖
watch(keyword, (val) => {
  if (frontendPage) {
    schedule(() => {
      // 前端模式：防抖后触发内存过滤
      tableData.value = filterOriginData(originData.value, val)
    })
  } else {
    schedule(() => {
      // 后端模式：防抖后触发后端请求
      forceSearch()
    })
  }
})

// 用户点击查询/回车/刷新 → 取消防抖 + 直接走后端
function forceSearch() {
  cancel()           // ← 核心：清除正在等待的防抖定时器
  loading.value = true
  fetchApi(params).then(data => {
    originData.value = data
    tableData.value = data
  }).finally(() => {
    loading.value = false
  })
}
```

### 5.6 QueryBar 改造

**改造原则：只改一个组件，不做新组件。** 防抖逻辑属于 Composable 层，UI 组件保持简单。

**改造点**:
1. 查询按钮和刷新按钮统一 emit `forceSearch`（不再区分 `search` / `refresh`）
2. 新增 `localFilterEmpty` prop，控制空结果引导提示的显隐
3. 移除 `fromCache` / `flashTick` 相关 props（因为 `forceSearch` 永远走后端，不再区分是否来自缓存）

**改造后的 QueryBar 模板关键部分**:
```html
<el-input
  v-model="model"
  :placeholder="placeholder"
  clearable
  @keyup.enter="emit('forceSearch')"
  @clear="emit('forceSearch')"
/>
<el-button type="primary" :icon="Search" @click="emit('forceSearch')">
  {{ $t('common.search') }}
</el-button>
<!-- 重置按钮（替代原刷新按钮位置，详见 §5.10） -->
<el-button @click="emit('reset')">
  {{ $t('common.reset') }}
</el-button>
<!-- 前端过滤为空时的引导性提示 -->
<el-tag v-if="localFilterEmpty" type="warning" size="small">
  {{ $t('common.localFilterEmpty') }}
</el-tag>
```

**关键细节**：
- `@clear="emit('forceSearch')"` — 当用户点击输入框的清空按钮时，自动触发一次后端查询，恢复全量数据。
- 查询按钮与刷新按钮同源，都触发 `forceSearch`；刷新图标移至右侧（`right` 插槽区域），详见 §5.10。

### 5.7 关于 `fromCache` 和 `flashTick` 的说明

改造后，查询按钮和刷新按钮都强制走后端，因此：
- `fromCache` prop 不再有意义（永远不会"来自缓存"）
- `flashTick` 闪烁提示保留，但用于标识"数据已刷新"（`dataSourceTick` 变化时触发）

如果未来需要保留缓存能力，可以通过 `enableCache` 参数控制，但"点击查询按钮"永远走 `forceRefresh=true`。

### 5.8 关于 `resetSearch` 的语义变化

**改造前**:
```typescript
function resetSearch() {
  keyword.value = ''
  current.value = 1
  fetchData()  // 可能走缓存
}
```

**改造后**:
```typescript
function resetSearch() {
  keyword.value = ''
  current.value = 1
  forceSearch()  // 强制走后端，恢复全量数据
}
```

### 5.9 模式 B 页面迁移建议

对于 `system/Users.vue`、`system/cache/index.vue`、`system/tasks/index.vue`、`system/config/index.vue` 等手动查询页面，建议统一迁移到 `useSmartQueryTable` + `QueryBar`：

**迁移前（Users.vue 为例）**:
```typescript
const keyword = ref('')
const loading = ref(false)
const users = ref<UserVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const load = async () => {
  loading.value = true
  try {
    const data = await fetchUsers({ current: current.value, size: size.value, keyword: keyword.value })
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}
```

**迁移后**:
```typescript
const {
  tableData: users,
  loading,
  keyword,
  current,
  size,
  total,
  forceSearch,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<UserVO>({
  fetchApi: (params) => fetchUsers(params),
  searchFields: ['username', 'email'],
  frontendPage: false,  // 服务端分页
  enableCache: true,
  debounceDelay: 500,
})
```

### 5.10 重置按钮布局调整

**背景**：改造后刷新按钮与查询按钮同源（都触发 `forceSearch`），原刷新按钮位置空出。同时，对于大数据量页面（后端分页），用户仍需要一个快速同步最新数据的入口——刷新图标（↻）保留在右侧 `right` 插槽区域。

**布局变化**：

```
改造前布局：
┌──────────────────────────────────────────────────────────────────────┐
│ [筛选控件...]  [输入框]  [🔍 查询]  [🔄 刷新]  [操作按钮...]          │
└──────────────────────────────────────────────────────────────────────┘

改造后布局：
┌──────────────────────────────────────────────────────────────────────┐
│ [筛选控件...]  [输入框]  [🔍 查询]  [↺ 重置]  [操作按钮...]  [🔄 刷新] │
└──────────────────────────────────────────────────────────────────────┘
                                      ▲                        ▲
                         原刷新按钮位置 → 重置           右侧 → 刷新图标
```

**关键变化**：
- 原刷新按钮位置 → 替换为**重置按钮**（清空输入条件）
- 右侧 `right` 插槽区域 → 保留纯图标**刷新按钮**（↻），与查询按钮同源触发 `forceSearch`
- 查询按钮和刷新按钮功能完全一致：都携带当前输入条件强制走后端

**刷新按钮的定位**：

| 页面类型 | 刷新按钮策略 |
|---|---|
| 大数据量页面（后端分页，数据高频变动） | 在右侧显示纯图标刷新按钮，方便用户不修改条件时快速同步 |
| 小数据量页面（前端全量，数据低频变动） | 刷新按钮可隐藏，因为前端防抖过滤已足够；也可保留作为手动同步入口 |

**通过 `showRefresh` prop 控制显隐**：

```html
<!-- 后端分页页面：显示刷新图标 -->
<QueryBar
  v-model:keyword="keyword"
  :show-refresh="true"
  @force-search="forceSearch"
  @reset="resetSearch"
/>

<!-- 前端全量页面：隐藏刷新图标（可选） -->
<QueryBar
  v-model:keyword="keyword"
  :show-refresh="false"
  @force-search="forceSearch"
  @reset="resetSearch"
/>
```

**分情况处理**：

| 当前页面状态 | 处理方式 |
|---|---|
| 无重置按钮 | 在刷新按钮原位置**新增**重置按钮 |
| 有重置按钮但在其他位置（如 `left` 插槽中） | 将重置按钮**移动**到刷新按钮原位置，从原位置移除 |
| 已有重置按钮且已在刷新按钮附近 | 保持不动 |

**重置按钮行为**：
- 清空关键字输入框
- 清空所有附加筛选条件（如有）
- 重置页码为第 1 页
- 自动触发一次 `forceSearch`（走后端恢复全量数据）

**QueryBar 改造后的完整模板**：

```html
<template>
  <div class="search-bar">
    <!-- 左侧：附加筛选条件（如 select、date-picker 等） -->
    <slot />

    <!-- 关键字输入框 -->
    <el-input
      v-model="model"
      :placeholder="placeholder"
      clearable
      @keyup.enter="emit('forceSearch')"
      @clear="emit('forceSearch')"
    />

    <!-- 查询按钮（合并了原刷新功能） -->
    <el-button type="primary" :icon="Search" @click="emit('forceSearch')">
      {{ $t('common.search') }}
    </el-button>

    <!-- 重置按钮（替代原刷新按钮位置） -->
    <el-button @click="emit('reset')">
      {{ $t('common.reset') }}
    </el-button>

    <!-- 前端过滤为空时的引导性提示 -->
    <el-tag v-if="localFilterEmpty" type="warning" size="small">
      {{ $t('common.localFilterEmpty') }}
    </el-tag>

    <!-- 弹性空间 -->
    <div class="flex-1" />

    <!-- 右侧：操作按钮（如新增、导出等） -->
    <slot name="right" />

    <!-- 刷新图标（纯图标，仅在大数据量页面显示，与查询按钮同源） -->
    <el-button
      v-if="showRefresh"
      :icon="Refresh"
      circle
      @click="emit('forceSearch')"
    />
  </div>
</template>
```

**对应 useSmartQueryTable 的 resetSearch 方法**：

```typescript
// 重置：清空 keyword + 重置页码 → 强制后端查询
function resetSearch() {
  keyword.value = ''
  current.value = 1
  // 如果有附加筛选条件过滤器，也一并重置
  // 例如：statusFilter.value = ''; typeFilter.value = ''
  forceSearch()
}
```

**页面侧使用示例**：

```html
<QueryBar
  v-model:keyword="keyword"
  :local-filter-empty="localFilterEmpty"
  :show-refresh="true"
  @force-search="forceSearch"
  @reset="resetSearch"
>
  <!-- 附加筛选条件放在 left 默认插槽 -->
  <el-select v-model="status" clearable @change="forceSearch">
    <el-option label="启用" :value="1" />
    <el-option label="禁用" :value="0" />
  </el-select>
</QueryBar>
```

```typescript
// 页面中 resetSearch 可以扩展，追加重置附加筛选条件
const { keyword, forceSearch, resetSearch: baseReset, localFilterEmpty } = useSmartQueryTable({...})

const status = ref('')

function resetSearch() {
  status.value = ''  // 重置附加筛选条件
  baseReset()        // 调用基础重置（清空 keyword + 重置页码 + forceSearch）
}
```

---

## 6. 大数量分页 vs 前端全量小数据的兼容策略

### 6.1 两种模式的本质区别

| 维度 | 后端大数量分页 | 前端全量小数据 |
|---|---|---|
| 数据量 | 成千上万条 | 几十到几百条 |
| 查询方式 | 每次翻页/搜索都请求后端 | 首次全量拉取，后续前端操作 |
| `originData` 内容 | 当前页的数据（只有一页） | 全量数据 |
| 前端过滤 | 不适用（数据不全，过滤结果会误导用户） | 适用（全量已在内存，秒级响应） |
| 防抖后行为 | 触发后端请求（带 keyword 参数） | 触发前端内存过滤（不请求后端） |
| 分页 | 后端分页 | 前端切片 |
| 配置 | `frontendPage: false` | `frontendPage: true` |
| 缓存 | 可选 | 推荐开启 |

### 6.2 ⚠️ 关键陷阱：后端分页模式下禁止前端防抖过滤

**错误做法**（会导致数据完整性问题）：
```typescript
// 后端分页模式，originData 只有当前页 10 条数据
// 用户输入 keyword，前端过滤 originData，只剩 2 条
// 用户以为只有 2 条匹配，但实际后端有 500 条匹配！
// → 严重误导用户
```

**正确做法**：通过 `frontendPage` 参数自动分流：

```typescript
// ===== 后端大数量分页模式 =====
// 特点：防抖输入 → 触发后端请求（带上 keyword 参数）
// 注意：此模式下 searchFields 配置仅用于前端辅助标记，不用于过滤
useSmartQueryTable({
  fetchApi: (params) => api.searchUsers(params),  // 后端分页 API
  frontendPage: false,   // 后端分页
  debounceDelay: 500,    // 防抖后触发后端请求
})

// ===== 前端全量小数据模式 =====
// 特点：首次全量拉取 → 防抖输入 → 前端内存过滤
useSmartQueryTable({
  fetchApi: () => api.listAll(),  // 全量 API
  frontendPage: true,    // 前端分页
  searchFields: ['name', 'code', 'desc'],  // 前端过滤字段
  debounceDelay: 500,    // 防抖后触发前端过滤
  enableCache: true,     // 3 分钟缓存
})
```

**内部逻辑分歧**:
```typescript
// 在 keyword watch 中，根据 frontendPage 自动分流
watch(keyword, (newVal) => {
  if (frontendPage) {
    // 前端全量模式：防抖后触发前端过滤（不请求后端）
    debounce.schedule(() => {
      tableData.value = filterOriginData(originData.value, newVal)
      localFilterEmpty.value = tableData.value.length === 0
    })
  } else {
    // 后端分页模式：防抖后触发后端请求（带 keyword）
    // 绝对不在这里做前端过滤，因为 originData 只有当前页数据
    debounce.schedule(() => {
      forceSearch()
    })
  }
})
```

### 6.3 建议的默认策略

| 场景 | 推荐模式 | 原因 |
|---|---|---|
| 系统配置、字典、任务列表 | 前端全量 | 数据量小（<500），全量拉取后前端过滤体验极佳 |
| 用户列表、审计日志、执行记录 | 后端分页 | 数据量大（可能上万），后端分页 + 关键字搜索 |
| 角色、权限、IP 规则 | 前端全量 | 配置类数据，通常 <200 条 |
| 作业、IFS、对象 | 前端全量 + 缓存 | AS400 查询较慢，缓存减少延迟 |

---

## 7. 实施路线图建议

### Phase 1：基础设施（✅ 已完成 — 2026-08-20）

1. ✅ 创建 `cancellableDebounce.ts`：支持 `schedule` / `cancel` / `flush` 的可取消防抖
2. ✅ 创建 `useSmartQueryTable.ts`：在 `useTablePage` 基础上封装防抖 + 强制后端逻辑
3. ✅ 改造 `QueryBar.vue`：统一查询/刷新为 `forceSearch` 事件，移除 `search`/`refresh` emit，增加 `reset` 事件和空结果引导提示
4. ✅ 补充 i18n 文案：`common.localFilterEmpty` = "未找到本地数据，可点击【查询】深度检索"（zh-CN + en-US）
5. ✅ 标记 `useTablePage.ts` 为 `@deprecated`，添加 JSDoc 引导迁移

**Phase 1 交付物清单**:

| 文件 | 状态 | 说明 |
|---|---|---|
| `src/utils/cancellableDebounce.ts` | ✅ 新增 | `createCancellableDebounce(delay)` → `{ schedule, cancel, flush }` |
| `src/composables/useSmartQueryTable.ts` | ✅ 新增 | 在 `useTablePage` 基础上封装，返回 `tableData`/`forceSearch`/`resetSearch`/`localFilterEmpty` |
| `src/composables/useTablePage.ts` | ✅ 保留 | `useSmartQueryTable` 内部依赖其底层能力，`stores/user.ts` 导入 `clearTablePageCache`；已更新 `@deprecated` JSDoc 说明当前状态 |
| `src/components/QueryBar.vue` | ✅ 已改造 | 仅 emit `forceSearch`（查询/回车/刷新同源）和 `reset`；内置重置按钮；移除 `search`/`refresh` emit |
| `src/i18n/lang/zh-CN/common.ts` | ✅ 已补充 | `localFilterEmpty: '未找到本地数据，可点击【查询】深度检索'` |
| `src/i18n/lang/en-US/common.ts` | ✅ 已补充 | `localFilterEmpty: 'No local data found, click [Search] for deep retrieval'` |

**一次性迁移（QueryBar 向后兼容）**:
- 所有 14 个使用 `QueryBar` 的页面已同步更新：`@search` → `@force-search`，`@refresh` → 移除（合并到 `@force-search`），新增 `@reset`
- 6 个页面从 slot 中移除了重复的 reset 按钮（QueryBar 已内置）
- 4 个页面（`messageFiles`/`bizData`/`sysvals`/`pf`）新增了 `resetSearch` 支持
- 移除了 `handleSearch` 变量引用（`scripts`/`executions` 改为使用 `onFilterChange` 调用 `handleRefresh`）
- `job/index.vue` 移除了废弃的 `searchJobs` 函数

**测试验证**:
- TypeScript 编译：✅ 零错误
- ESLint：✅ 零错误
- Vitest：✅ 6 个测试文件全部通过（60 passed | 20 skipped — bash 门禁测试在 Windows 自动跳过）
- `gates.test.ts`：✅ 跨平台兼容修复（bash 不可用时 20 个测试自动 skip，不再报 FAIL）

### Phase 2：新页面试点（✅ 已完成）

1. ~~选 1~2 个模式 A 页面（如 `job/index.vue` 或 `schedule/index.vue`）迁移到 `useSmartQueryTable`~~
2. ~~选 1 个模式 B 页面（如 `system/Users.vue`）迁移到新方案~~
3. ~~验证功能正确性 + 体验流畅度 + 回归测试~~

> 实际一次性完成全部迁移，未分步试点。

### Phase 3：全面推广（✅ 已完成）

1. ~~模式 A 的 19 个页面逐步迁移~~ → **全部 19 个页面已迁移至 `useSmartQueryTable`**
2. ~~模式 B 的 6 个页面逐步迁移~~ → **4 个页面已迁移，2 个页面跳过（特殊布局，见下方说明）**
3. ~~回归测试~~ → **全部通过**

> **模式 B 迁移详情：**

| 页面 | 状态 | 迁移方式 | 说明 |
|------|------|----------|------|
| `system/cache/index.vue` | ✅ 已迁移 | `frontendPage: true`, `searchFields: ['name']` | 替换手动 `computed` 过滤分页 + `load()` 函数 |
| `system/tasks/index.vue` | ✅ 已迁移 | `frontendPage: true`, `searchFields: ['bean', 'className']` | 同上 |
| `system/config/index.vue` | ✅ 已迁移 | `frontendPage: true`, `searchFields: ['configKey', 'description']` | 同上，保留对话框表单逻辑 |
| `system/Users.vue` | ✅ 已迁移 | `frontendPage: false`, `enableCache: false` | 替换手动后端分页 `fetchUsers()` 调用，保留多 Tab 布局（security tab 的 attempts/ipStats 为简单列表无关键字过滤分页，无需迁移） |
| `system/webhooks/index.vue` | ⏭️ 跳过 | 不适用 | 见下方详细说明 |
| `system/dict/index.vue` | ⏭️ 跳过 | 不适用 | 见下方详细说明 |

> **⏭️ 跳过页面的详细原因：**

> **`system/webhooks/index.vue`** — 双 Tab 页面，每个 Tab 是独立的数据源和交互模式：
> - **config Tab**：Webhook 配置列表，无关键字搜索输入框，只有创建按钮 + 开关切换 + 测试/编辑/删除操作。数据量小，`loadWebhooks()` 一次性全量加载，无分页。
> - **logs Tab**：Webhook 执行日志，有自定义搜索栏：`logName`（webhook 名称关键字）+ `logSuccess`（成功/失败状态筛选），后端分页。这是双条件组合筛选器，不是单一关键字输入框模式。
> - **结论**：两个 Tab 都不是标准的「关键字输入框 → 模糊匹配 → 分页表格」模式，强行用 `useSmartQueryTable` 需要为每个 Tab 单独实例化且需处理双条件筛选，反而增加复杂度。

> **`system/dict/index.vue`** — 主从布局（Master-Detail），非标准表格页：
> - **左面板（字典类型）**：`typeKeyword` 关键字过滤 + `filteredTypes` computed，点击行选中类型。这是一个简单的列表筛选，不是 `el-table` + 分页的组合。
> - **右面板（字典项）**：选中类型后加载对应的字典项列表（`loadItems(typeCode)`），无关键字搜索，无分页。数据完全依赖左面板的选中状态。
> - **结论**：整体是「类型选择 → 字典项加载」的主从联动模式，与 `useSmartQueryTable` 的「关键字过滤 + 分页表格」模式不匹配。

> **迁移效果：** 所有已迁移页面功能不变，仅底层实现从手动 `ref`/`computed`/`load()` 替换为 `useSmartQueryTable`。增加能力：输入关键字 500ms 防抖自动过滤，查询/回车/刷新统一走 `forceSearch()` 强行后端查询。

### 多 Tab 页面排查：确认无遗漏

> 对项目中所有含 `el-tabs` 的页面进行了全面排查，确认每个 Tab 是否需要但未使用 `useSmartQueryTable`：

| 页面 | Tab 结构 | 排查结论 |
|------|----------|----------|
| `job/index.vue` | msgw / lckw / other（3 个独立表格） | ✅ 3 个 Tab 均已使用 `useSmartQueryTable`（3 个独立实例） |
| `docs/index.vue` | normal / deleted（视图切换） | ✅ 已使用 `useSmartQueryTable`，Tab 仅切换数据源 |
| `objects/index.vue` | 主表格 + drawer 内 refIn / refOut / authorities | ✅ 主表格已使用 `useSmartQueryTable`；drawer 内 Tab 为点击行后按需加载的小型引用列表，无关键字过滤和分页 |
| `system/Users.vue` | users / security | ✅ users Tab 已使用 `useSmartQueryTable`；security Tab 的 attempts/ipStats 为简单列表（无关键字过滤、无分页），无需迁移 |
| `system/params/index.vue` | config / dict / permissions | ⏭️ 非表格页，是嵌入 3 个已有子组件（`SysConfig`/`DictManage`/`Permissions`）的容器页，各子组件内部已自行处理查询逻辑 |
| `system/permissionRequest/index.vue` | mine / approvals | ⏭️ 权限申请工作流页面（菜单树 + 多步表单 + 审批），非标准表格页 |
| `system/webhooks/index.vue` | config / logs | ⏭️ 见上方详细说明 |
| `topology/index.vue` | 拓扑图 + drawer 内 refIn / refOut | ⏭️ 可视化拓扑图页面，主区域为 ECharts 图表非表格；drawer 内 Tab 为小型引用列表 |
| `report/index.vue` | manual / schedule | ⏭️ manual Tab 为表单控件（无表格）；schedule Tab 为简单列表（无关键字过滤、无分页） |
| `system/menus/MenuFormDialog.vue` | 对话框内 Tab | ⏭️ 对话框组件，非页面 |
| `system/UserPermDialog.vue` | 对话框内 Tab | ⏭️ 对话框组件，非页面 |

> **结论：所有适合使用 `useSmartQueryTable` 的表格 Tab 均已迁移，无遗漏。**

### Phase 4：清理（✅ 已完成 — 但 `useTablePage.ts` 保留）

1. ~~移除 `useTablePage.ts`~~ → **保留**，原因：
   - `useSmartQueryTable.ts` 内部依赖 `useTablePage` 提供底层能力（`fetchData`、`handlePageChange`、`handleSizeChange` 等）
   - `stores/user.ts` 导入 `clearTablePageCache` 用于退出登录时清除全部页面缓存
   - `useTablePage.test.ts` 直接测试 `useTablePage` 核心逻辑
   - 二者关系：`useSmartQueryTable` = `useTablePage` + 防抖层 + 智能过滤层
2. ~~移除所有页面中的旧手动查询代码~~ → **✅ 已完成**：所有 `views/` 目录下零残留 `useTablePage` 导入，零残留手动 `computed` 过滤分页代码
3. ~~统一所有页面的查询交互体验~~ → **✅ 已完成**：所有页面统一使用 `forceSearch`（查询/回车/刷新同源）+ `resetSearch`（重置）

---

## 附录 A：所有页面查询模式速查表（已全部迁移至 useSmartQueryTable）

| 页面文件 | 模式 | QueryBar | useSmartQueryTable | frontendPage | enableCache | searchFields | 防抖 |
|---|---|---|---|---|---|---|---|
| `job/index.vue` (×3) | A | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/roles/index.vue` | A | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `audit/index.vue` | A | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `objects/index.vue` | A | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `ifs/index.vue` | A | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `schedule/index.vue` | A | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `data/sysvals/index.vue` | A | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/i18n/index.vue` | A | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `system/ipRules/index.vue` | A | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `system/loginLog/index.vue` | A | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `system/notice/index.vue` | A | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `system/permissions/index.vue` | A | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `data/messageFiles/index.vue` | A | - | ✅ | ❌ | ✅ | ✅ | ✅ |
| `docs/index.vue` | A | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `subsystems/index.vue` | A | - | ✅ | ✅ | ✅ | ✅ | ✅ |
| `scripts/index.vue` | A | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `executions/index.vue` | A | - | ✅ | ❌ | ✅ | ✅ | ✅ |
| `biz/data/index.vue` | A | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| `pf/index.vue` | A | - | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/Users.vue` | B | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |
| `system/cache/index.vue` | B | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/tasks/index.vue` | B | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/config/index.vue` | B | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system/webhooks/index.vue` | B | ❌ | ⏭️ | - | - | - | - |
| `system/dict/index.vue` | B | ❌ | ⏭️ | - | - | - | - |
| `query/index.vue` | C | ❌ | ⏭️ | - | - | - | - |
| `report/index.vue` | C | ❌ | ⏭️ | - | - | - | - |
| `Monitor.vue` | C | ❌ | ⏭️ | - | - | - | - |

> 图例: ✅ = 已使用, ❌ = 未使用/不适用, ⏭️ = 跳过（特殊布局不适合迁移）, - = 不适用

---

## 附录 B：当前 i18n 文案清单（需补充项）

| Key | 中文 | English | 用途 |
|---|---|---|---|
| `common.search` | 查询 | Search | 已有 |
| `common.refresh` | 刷新 | Refresh | 已有 |
| `common.reset` | 重置 | Reset | 已有 |
| `common.noData` | 暂无数据 | No Data | 已有 |
| `common.fromCache` | 来自缓存 | From Cache | 已有（改造后不再使用，保留不删） |
| `common.localFilterEmpty` | 未找到本地数据，可点击【查询】深度检索 | No local data found, click [Search] for deep retrieval | **需新增** |

---

> **文档结束** — 下一步：Review 本分析文档，确认方案后进入 Phase 1 代码实现。