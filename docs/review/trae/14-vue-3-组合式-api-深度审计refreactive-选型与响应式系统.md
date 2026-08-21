---
title: "十四、Vue 3 组合式 API 深度审计：ref/reactive 选型与响应式系统"
---

# 十四、Vue 3 组合式 API 深度审计：ref/reactive 选型与响应式系统

> **审计日期**：2026-08-15（第七轮专项审计 —— 组合式 API 响应式系统）

---

### 14.1 ref vs reactive 使用统计与选型分析

#### [良好实践] `ref` 使用一致性强，基本类型全部使用 `ref()`

**全景统计**：

| 数据类型                          | 使用方式          | 文件数 | 评价      |
| --------------------------------- | ----------------- | ------ | --------- |
| 基本类型（string/number/boolean） | 统一使用 `ref()`  | 50+    | ✅ 正确   |
| 对象/数组（表单数据）             | 使用 `reactive()` | 13 处  | ✅ 正确   |
| 对象/数组（加载状态）             | 使用 `ref()`      | 40+    | ✅ 可接受 |
| 模板引用（`ref` 属性）            | 使用 `ref()`      | 15+    | ✅ 正确   |
| `computed` getter                 | 使用 `computed()` | 20+    | ✅ 正确   |

**选型分析**：

```typescript
// ✅ 正确模式 A：基本类型用 ref
const loading = ref(false);
const keyword = ref("");
const current = ref(1);
const total = ref(0);

// ✅ 正确模式 B：表单对象用 reactive（需要整体替换时用 Object.assign）
const form = reactive({ name: "", code: "", description: "" });
// 重置表单：Object.assign(form, defaultForm())

// ✅ 正确模式 C：对话框状态用 ref（null 初始值）
const editData = ref<UserEditData | null>(null);

// ✅ 正确模式 D：props 双向绑定用 computed getter/setter
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});
```

#### [良好实践] 项目中 `reactive()` 使用规范，集中在表单场景

**13 处 `reactive()` 使用清单**：

| 文件                       | 变量               | 用途            | 评价    |
| -------------------------- | ------------------ | --------------- | ------- |
| `Login.vue`                | `form`             | 登录表单        | ✅ 正确 |
| `UserFormDialog.vue`       | `form`             | 用户表单        | ✅ 正确 |
| `RoleFormDialog.vue`       | `form`             | 角色表单        | ✅ 正确 |
| `MenuFormDialog.vue`       | `form`             | 菜单表单        | ✅ 正确 |
| `config/index.vue`         | `form`             | 配置表单        | ✅ 正确 |
| `docs/index.vue`           | `form` + `filters` | 文档表单 + 筛选 | ✅ 正确 |
| `EventFormDialog.vue`      | `form`             | 日历事件表单    | ✅ 正确 |
| `TemplateManageDialog.vue` | `form`             | 模板表单        | ✅ 正确 |
| `region/index.vue`         | `form`             | 区域表单        | ✅ 正确 |
| `Source.vue`               | `compileForm`      | 编译表单        | ✅ 正确 |
| `audit/index.vue`          | `filters`          | 审计筛选        | ✅ 正确 |
| `sysvals/index.vue`        | `form`             | 系统值修改表单  | ✅ 正确 |

**结论**：项目中 `reactive()` 的使用非常规范——仅在表单对象场景使用，没有混用 `ref()` 包裹对象的情况。这是 Vue 3 组合式 API 的最佳实践。

#### [重构建议/Info] 部分组件可进一步将表单逻辑抽取为 composable

```typescript
// 建议：抽取通用表单 composable
// composables/useForm.ts
export function useForm<T extends Record<string, unknown>>(defaults: () => T) {
  const form = reactive(defaults()) as T;
  const reset = () => Object.assign(form, defaults());
  return { form, reset };
}

// 使用方简化
const { form, reset } = useForm(() => ({ name: "", code: "" }));
```

---

### 14.2 响应式解构与 `toRefs` 使用审计

#### [良好实践] Pinia store 使用 `storeToRefs()` 解构，避免失去响应式

**文件**：[TagsView.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/layout/TagsView.vue#L71-L73)

```typescript
// 正确：使用 storeToRefs 保持响应式
import { storeToRefs } from "pinia";
const tagsStore = useTagsStore();
const { visitedViews, cachedViews } = storeToRefs(tagsStore);
```

#### [规范警告/Warning] `TagsView.vue` 中存在手动同步 `visitedViews` 的问题

> **AAA-Remark（2026-08-15 复核）**：与 §4.2 为**同一问题**（重复提及）。验证与建议见 §4.2 备注——直接 `storeToRefs` 解构即可，保留局部过滤/排序逻辑。

**文件**：[TagsView.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/layout/TagsView.vue#L71-L73)

```typescript
// 原代码：创建本地 ref 副本，操作后手动同步
const visitedViews = ref<TagView[]>(tagsStore.visitedViews);
// 多处操作后：
visitedViews.value = tagsStore.visitedViews; // 手动同步，反复出现
```

**建议**：直接使用 `storeToRefs` 解构的 `visitedViews`，通过 store actions 修改状态，消除手动同步。

---

### 14.3 `watch` / `watchEffect` 使用审计

#### [良好实践] `watch` 使用克制，未发现冗余监听

**审计结果**：项目中 `watch` 使用集中在以下合法场景：

- 路由参数变化时重新加载数据（`watch(() => route.params.id, ...)`）
- 分页参数变化时自动重新查询（`useTablePage` 内部）
- 对话框 `visible` 变化时初始化/重置表单
- `keyword` 变化时触发防抖搜索

未发现"watch 链式调用"或"在 watch 中修改被监听值"等反模式。

---

### 14.4 生命周期钩子使用规范

#### [良好实践] 所有 `onMounted` / `onBeforeUnmount` 成对出现

**审计结果**（复用 §10.3 数据）：

| 组件/Composable      | 注册资源                                                  | 清理位置                          | 状态        |
| -------------------- | --------------------------------------------------------- | --------------------------------- | ----------- |
| 10 个组件/Composable | `setInterval` / `addEventListener` / `setTimeout` / STOMP | `onBeforeUnmount` / `onUnmounted` | ✅ 全部正确 |

**结论**：项目在响应式系统和生命周期管理方面整体规范，`ref`/`reactive` 选型合理，`computed` 使用恰当，生命周期钩子成对使用。主要改进点是将 `TagsView.vue` 的手动同步改为 `storeToRefs` 直接解构。

---
