---
title: "四、Vue 3 组合式 API 与状态流向"
---

# 四、Vue 3 组合式 API 与状态流向

### 4.1 setup 复杂度审计

#### [规范警告/Warning] `Users.vue` setup 逻辑体量偏大

> **AAA-Remark（2026-08-15 复核）**：✅ 问题大体真实——`<script setup>` 约 152 行（文档 160 接近）、10 个 `ref`（文档 15 偏多）。`editData` 仍是内联对象类型，但已引用具名类型（`SysRole[]` + `UserStatus`）；插槽类型化专项（Action 2）后行类型已 100% 收敛，页面级 composable 抽取仍未做，属 Info 级。→ **状态：行类型标注 ✅ 已闭环（Action 2，144/144）；composable 抽取 ⏳ 未做**

**文件**：[Users.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/system/Users.vue#L129-L291)

**统计**：`<script setup>` 区域约 160 行，包含 15 个 `ref` 声明、7 个异步函数、2 个纯函数，全部内联在组件中。新人在阅读时需要在 `load()` → `loadRoles()` → `loadAttempts()` → `loadIpStats()` 之间频繁跳转。

**问题**：`editData` 的类型定义是内联复杂对象而非独立 interface：

```typescript
// 原代码：内联类型，不可复用
const editData = ref<{
  id: number;
  username: string;
  email: string;
  roles: { id: number }[];
  status: string;
} | null>(null);
```

**重构建议**：抽取 `UserEditData` 类型 + `useUserManager` composable。

```typescript
// 易读重构：types/user.ts
export interface UserEditData {
  id: number;
  username: string;
  email: string;
  roles: { id: number }[];
  status: string;
}

// composables/useUserManager.ts
export function useUserManager() {
  const users = ref<UserVO[]>([]);
  const total = ref(0);
  const loading = ref(false);
  // ...

  const load = async () => {
    /* ... */
  };
  const toggleStatus = async (row: UserVO) => {
    /* ... */
  };
  const remove = async (row: UserVO) => {
    /* ... */
  };

  return { users, total, loading, load, toggleStatus, remove };
}

// Users.vue 简化为
const { users, total, loading, load, toggleStatus, remove } = useUserManager();
```

### 4.2 Pinia 状态流向审计

#### [良好实践] `useUserStore` 状态管理清晰

> **AAA-Remark（2026-08-15 复核）**：✅ 确认为良好实践——状态变更仅经 actions、`menusFetchPromise` 并发去重、`hasPermission`/`canSeeTab` 纯函数 getter，均与文档描述一致。

**文件**：[user.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/stores/user.ts)

- 状态变更仅通过 `actions`（`login`, `logout`, `applyLogin`, `fetchMenus`）
- `getters` 中的 `hasPermission` 和 `canSeeTab` 是纯函数，链路清晰
- `menusFetchPromise` 并发去重设计合理，且有注释说明

#### [重构建议/Info] `TagsView.vue` 中 `visitedViews` 同步模式

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——5 处 `visitedViews.value = tagsStore.visitedViews` 手动同步（L97/129/135/141/170）。与 §14.2 重复提及，属同一问题。改 `storeToRefs` 直接解构即可，注意 TagsView 还有局部过滤/排序逻辑，改造时保留。

**文件**：[TagsView.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/layout/TagsView.vue#L71-L73)

```typescript
// 原代码：组件内维护 visitedViews 的 ref 副本，每次操作后手动同步
const visitedViews = ref<TagView[]>(tagsStore.visitedViews);
// ... 操作后
visitedViews.value = tagsStore.visitedViews; // 手动同步，反复出现
```

**建议**：使用 `storeToRefs` 直接解构或使用 `computed` 替代手动同步。

### 4.3 `useTablePage` composable 审计

#### [良好实践] 设计良好的通用 composable

> **AAA-Remark（2026-08-15 复核）**：✅ 确认为良好实践——缓存（3 分钟 TTL + route/params/serverId 隔离）、AbortController 去重、前端/后端分页双模式均属实；已有 20+ 页面使用（§8.7 的推广建议成立）。

**文件**：[useTablePage.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useTablePage.ts)

这是项目中可复用性最好的 composable，支持：

- 前端分页 / 后端分页双模式
- 前端模糊搜索（`searchFields`）
- 查询缓存（3 分钟 TTL，按 route+params+serverId 隔离）
- 表格高度自适应
- 请求去重（`AbortController`）

**建议**：将 `useTablePage` 作为"优秀 composable 模板"向团队推广，其他业务页面（如 Webhook、Notice、Doc）应参照此模式抽取。

---
