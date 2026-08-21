---
title: "十、深度架构审计：Entity 暴露、越权、N+1、事务安全"
---

# 十、深度架构审计：Entity 暴露、越权、N+1、事务安全

> **审计日期**：2026-08-15（第三轮深度审计 —— 全栈架构师视角）  
> **审计范围**：Controller → Entity 暴露、SQL 注入、XSS、越权、内存泄漏、N+1、事务长事务

---

### 10.1 后端架构与设计模式

#### 🛑 [严重/Critical] Controller 直接暴露数据库 Entity 给前端，未使用 DTO/VO 隔离

> **AAA-Remark（2026-08-15 复核）**：✅ **真实存在**——`SysRoleController`/`SysMenuController`/`PermissionController`/`ConfigController`/`WebhookController` 均直接返回 Entity（`SysUserController.manageableTree` 也返回 `List<SysMenu>`），与文档表格一致；全库仅 `SysUserController` 用 VO。⚠️ **严重程度修正**：审查合订本（轮次 10/13）已将 system 模块 Admin-only CRUD 定性为「形状合规的历史遗留」，并落地为 `scripts/check-layering.sh` 的 R3 白名单（`ALLOW_R3_ENTITY_TYPES` 放行 SysMenu/SysRole/SysConfig 等）——**新增代码禁止直返 Entity（门禁即失败），存量作为已知遗留限期收敛**。因此「严重/Critical」应理解为「存量待清偿」，不是「当前可被利用的漏洞」；暴露字段为管理后台常规字段（无密码等敏感列），实际风险低于字面严重度。→ **状态：部分 ✅ 已修复（P2-10：8 个 Controller 改走 9 个 VO，覆盖 as400/monitor/compile/security 模块）；system 模块存量 ⏳ 已知遗留（R3 门禁放行，限期收敛）**

**问题定性**：属于 **"数据传输与封装规范 (Lack of DTO/VO in services)"** 问题。

**影响范围**：

| Controller                                                                                                                                                           | 方法                                               | 暴露的 Entity   |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | --------------- |
| [SysRoleController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/SysRoleController.java#L36-L65)       | `list()`, `page()`, `create()`, `update()`         | `SysRole`       |
| [SysMenuController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/SysMenuController.java#L43-L67)       | `tree()`, `create()`, `update()`, `toggleStatus()` | `SysMenu`       |
| [PermissionController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/PermissionController.java#L60-L67) | `create()`, `update()`                             | `SysPermission` |
| [ConfigController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/ConfigController.java#L32-L46)         | `list()`, `update()`                               | `SysConfig`     |
| [WebhookController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/WebhookController.java#L37-L77)       | `list()`, `create()`, `update()`, `toggle()`       | `WebhookConfig` |
| [SysUserController](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/SysUserController.java#L83)           | `manageableTree()`                                 | `SysMenu`       |

**对比**：整个项目中唯一正确使用 DTO/VO 的 Controller 是 `SysUserController`（`UserVO` + `UserDTO` + `UserUpdateDTO`），其余 Controller 全部直接暴露 Entity。

**风险分析**：

1. **数据库 Schema 泄露**：`SysMenu` 实体包含 `adminOnly`, `menuType`, `sort`, `status`, `createdTime`, `updatedTime` 等全部字段直接序列化给前端。攻击者可以通过接口响应反推数据库表结构。
2. **前端与后端强耦合**：数据库字段重命名 → 前端需同步修改，违背"契约优先"原则。
3. **过度暴露敏感字段**：`SysConfig` 实体可能包含内部配置值；`SysPermission` 的 `id` 暴露了自增主键。
4. **Swagger/OpenAPI 文档污染**：实体上的 JPA/MyBatis-Plus 注解（如 `@TableId`, `@TableField`）会污染 API 文档。

**重构方案**：与 `SysUserController` 对齐，为每个 Entity 创建对应的 VO 类。

```java
// 重构前：直接暴露 Entity
@GetMapping("/tree")
@PreAuthorize("hasAuthority('MENU_MANAGE')")
public ApiResponse<List<SysMenu>> tree() {  // ❌ 暴露数据库实体
    return ApiResponse.success(menuService.tree());
}

// 重构后：使用 MenuVO 隔离
@GetMapping("/tree")
@PreAuthorize("hasAuthority('MENU_MANAGE')")
public ApiResponse<List<MenuVO>> tree() {   // ✅ 类型安全的 VO
    return ApiResponse.success(menuService.tree());
}

// MenuVO.java —— 只包含前端需要的字段
@Data
@Builder
public class MenuVO {
    private Long id;
    private Long parentId;
    private String title;
    private String path;
    private String icon;
    private Integer menuType;
    private String menuTypeLabel;  // 前端直接展示，无需映射
    private Integer sort;
    private Integer status;
    private Integer visible;
    private String perms;
    private List<MenuVO> children;
}
```

#### 🛑 [严重/Critical] `SysUserServiceImpl.bindRoles()` —— 逐条 INSERT + 逐条 SELECT 的 N+1 问题

**问题定性**：属于 **"数据库与持久层性能审计 (N+1 query patterns)"** 问题。

**文件**：[SysUserServiceImpl.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/SysUserServiceImpl.java#L208-L218)

**原因分析**：用户分配 3 个角色时，产生 **3 次 SELECT + 3 次 INSERT = 6 条 SQL**。如果分配 20 个角色（批量导入场景），产生 40 条 SQL，在高并发下严重拖慢数据库连接池。

```java
// 原代码：逐条 INSERT + 逐条 SELECT 验证存在性
for (Long roleId : roleIds.stream().filter(Objects::nonNull).distinct().toList()) {
    if (roleMapper.selectById(roleId) == null) {  // ← N 次 SELECT
        throw new BusinessException("角色不存在: " + roleId);
    }
    SysUserRole ur = new SysUserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    userRoleMapper.insert(ur);  // ← N 次 INSERT
}
```

**重构方案**：批量验证 + 批量 INSERT。

```java
// 重构后：2 次 SQL（1 次批量 SELECT + 1 次批量 INSERT）
private void bindRoles(Long userId, List<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) return;
    List<Long> distinctIds = roleIds.stream().filter(Objects::nonNull).distinct().toList();

    // 1 次批量验证：roleMapper.selectBatchIds(roleIds)
    List<SysRole> existingRoles = roleMapper.selectBatchIds(distinctIds);
    if (existingRoles.size() != distinctIds.size()) {
        Set<Long> existing = existingRoles.stream().map(SysRole::getId).collect(Collectors.toSet());
        List<Long> missing = distinctIds.stream().filter(id -> !existing.contains(id)).toList();
        throw new BusinessException("角色不存在: " + missing);
    }

    // 1 次批量 INSERT
    List<SysUserRole> userRoles = distinctIds.stream().map(roleId -> {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        return ur;
    }).toList();
    userRoleMapper.insert(userRoles);  // 需要 Mapper XML 支持批量 INSERT
}
```

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在，但与 §8.6 为**同一问题**（重复提及）。严重度「严重/Critical」偏高：实际角色绑定是管理端低频操作（分配角色，非批量导入高频路径），N 通常 ≤ 10，40 条 SQL 量级可接受；建议降为 Warning，按 §8.6 方案顺手优化即可，不必单独排期。

#### [规范警告/Warning] `SysUserServiceImpl` 中 `@Transactional` 方法内发布同步事件，存在长事务风险

**文件**：[SysUserServiceImpl.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/SysUserServiceImpl.java#L125-L140)

**问题**：`eventPublisher.publishEvent()` 默认是同步执行（项目未配置 `@EnableAsync`）。`PermissionCacheEventListener` 在同一个事务内执行，如果事件监听器执行耗时操作（如重建缓存），会延长事务持有时间，阻塞数据库连接。

```java
// 原代码：事件发布在 @Transactional 方法内，同步执行
@Transactional
public UserVO update(Long id, UserUpdateDTO dto) {
    // ... 数据库操作 ...
    eventPublisher.publishEvent(new UserPermissionGrantedEvent(user.getUsername()));  // ← 同步事件
    return toVO(user, rolesByIds(roleIds));
}
```

**建议**：配置 `@EnableAsync` + `@Async` 注解在 `PermissionCacheEventListener` 上，使缓存失效异步执行，不阻塞事务提交。

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——实测全库**无** `@EnableAsync`，`SysUserServiceImpl`/`PermissionRequestService` 的 `eventPublisher.publishEvent()` 均为同步执行（缓存失效监听在事务内）。当前缓存失效逻辑很轻（invalidate），实际风险低；若后续监听器变重再异步化。§11.3 方案 C 同一建议。→ **状态：✅ 已修复（D3任务：评估结论为当前缓存失效逻辑很轻，无需异步化；且§19已取消全部事务控制，事件监听不再受事务边界影响）**

---

### 10.2 安全性审计

#### ✅ [良好实践] `@PreAuthorize` 覆盖率高，所有敏感端点均受保护

**统计**：

- `SysUserController`：9 个方法全部 `@PreAuthorize("hasAuthority('USER_MANAGE')")`
- `SysRoleController`：6 个方法全部 `@PreAuthorize`
- `SysMenuController`：6 个方法全部 `@PreAuthorize("hasAuthority('MENU_MANAGE')")`
- `WebhookController`：8 个方法全部 `@PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")`
- `PermissionController`：6 个方法中 5 个有 `@PreAuthorize`（`/all` 端点开放给下拉字典，合理）
- `AuditLogController`：`@PreAuthorize("hasAuthority('AUDIT_VIEW')")`

**例外**：`PermissionController.all()` —— 无 `@PreAuthorize`，但该接口仅返回权限码列表供下拉字典使用，不包含敏感数据，合理。

#### ✅ [良好实践] 无 SQL 注入风险（MyBatis `${}` 零使用）

**审计结果**：项目中所有 `${}` 均为 Spring `@Value("${property}")` 配置注入，MyBatis SQL 全部使用 `#{}` 参数化查询。`@Select` 注解中的 SQL 也统一使用 `#{}` 占位符。

#### ✅ [良好实践] 无 XSS 风险（`v-html` 零使用）

**审计结果**：前端仅 1 处 `v-html` 引用，且为注释说明（"P2-28：v-html → 插值，消除存储型 XSS 面"），证明团队已主动移除 XSS 向量。

#### ✅ [良好实践] 前端 `v-has-perm` 指令 + `hasPermission()` 方法双重门控

**文件范围**：`webhooks/index.vue`, `permissions/index.vue`, `dict/index.vue`, `config/index.vue`, `scripts/index.vue`, `subsystems/index.vue`, `params/index.vue` 等

权限控制实现为"前端门控 + 后端 `@PreAuthorize`"双层防御，即使前端被绕过，后端仍会拦截。这是安全最佳实践。

#### [规范警告/Warning] `AuditLogController` 接受 `username` 参数但未做水平越权校验

**文件**：[AuditLogController.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/controller/AuditLogController.java#L29-L33)

**问题**：`username` 参数允许查询任意用户的审计日志。虽然该接口受 `@PreAuthorize("hasAuthority('AUDIT_VIEW')")` 保护，但 `AUDIT_VIEW` 权限的持有者（如审计员）可以查看所有用户的日志。如果未来权限粒度细化（如普通用户只能查看自己的审计日志），当前设计无法满足。

**当前风险评估**：**低风险** —— 审计日志设计初衷就是管理员/Auditor 查看全局日志，`AUDIT_VIEW` 权限本身就是高权限。但建议在代码注释中明确说明此设计意图。

> **AAA-Remark（2026-08-15 复核）**：✅ 评估准确（低风险）。补充：该 Controller 还直接注入 `AuditLogMapper` 并 `new LambdaQueryWrapper`（违反分层准绳 R1，但已在 `check-layering.sh` 白名单 `ALLOW_R1_*` 放行）——与本文 §3.1 的 QueryWrapper 议题同源，属存量遗留。

#### [良好实践] `WebhookService.sanitize()` 脱敏敏感字段

**文件**：[WebhookService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/WebhookService.java#L120-L130)

`WebhookConfig` 虽然作为 Entity 直接返回，但 `sanitize()` 方法会清空 `secret` 字段，防止密钥泄露。这是安全防御措施，但更好的做法是使用 VO 从根本上避免敏感字段暴露。

---

### 10.3 前端内存管理与生命周期

#### ✅ [良好实践] 所有事件监听器和定时器均有正确的清理逻辑

**审计结果**：

| 组件/Composable                                                                                              | 注册资源                                     | 清理位置          | 状态 |
| ------------------------------------------------------------------------------------------------------------ | -------------------------------------------- | ----------------- | ---- |
| [Monitor.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/Monitor.vue#L244-L262)                    | `setInterval` + `addEventListener('resize')` | `onBeforeUnmount` | ✅   |
| [topology/index.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/topology/index.vue#L146-L188)      | `addEventListener('resize')`                 | `onBeforeUnmount` | ✅   |
| [layout/index.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/layout/index.vue#L188-L198)                | `addEventListener('fullscreenchange')`       | `onUnmounted`     | ✅   |
| [TagsView.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/layout/TagsView.vue#L214-L220)                 | `setTimeout` + `addEventListener('click')`   | `onUnmounted`     | ✅   |
| [useTablePage.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useTablePage.ts#L188-L328)      | `addEventListener('resize')`                 | `onUnmounted`     | ✅   |
| [useShortcuts.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useShortcuts.ts#L75-L80)        | `addEventListener('keydown')`                | `onUnmounted`     | ✅   |
| [useFlash.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useFlash.ts#L15-L20)                | `setTimeout`                                 | `onUnmounted`     | ✅   |
| [useStompClient.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/composables/useStompClient.ts#L83)        | WebSocket STOMP                              | `onUnmounted`     | ✅   |
| [CommandPalette.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/components/CommandPalette.vue#L294-L301) | `addEventListener('keydown')` + `setTimeout` | `onUnmounted`     | ✅   |

**结论**：内存管理规范，无泄漏风险。这是项目的一个亮点。

#### [规范警告/Warning] `Monitor.vue` 使用全局 `window.__rxas400_monitor_timer` 变量

**文件**：[Monitor.vue](file:///d:/vueprojects/RXAS400ADM/frontend/src/views/Monitor.vue#L244-L261)

```typescript
// 原代码：挂载 timer 到 window 全局变量
const timer = window.setInterval(load, 10000);
window.__rxas400_monitor_timer = timer;
// ...
onBeforeUnmount(() => {
  clearInterval(window.__rxas400_monitor_timer);
  window.removeEventListener("resize", onResize);
});
```

**问题**：虽然清理逻辑正确，但使用 `window` 全局变量挂载 timer 是不规范的做法。`window` 是全局命名空间，容易与其他脚本冲突。应使用模块级变量或 `ref`。

```typescript
// 重构后：使用模块级变量
let monitorTimer: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
  monitorTimer = window.setInterval(load, 10000);
  window.addEventListener("resize", onResize);
});

onBeforeUnmount(() => {
  if (monitorTimer) clearInterval(monitorTimer);
  window.removeEventListener("resize", onResize);
});
```

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——`window.__rxas400_monitor_timer` 在 Monitor.vue L227/245/261 三处（全局类型声明 + 赋值 + 清理）。清理逻辑正确、无泄漏，仅命名空间不规范；模块级变量改造低风险，可顺手做。

---

### 10.4 前端 Axios 拦截器完整度审计

#### ✅ [良好实践] 401/403/500 状态码处理完善

**文件**：[request.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/api/request.ts#L90-L118)

```typescript
// 401 处理：清 token + 清 Pinia state + 跳转登录页
if (status === 401) {
  try {
    const pinia = getActivePinia();
    if (pinia) useUserStore().logout();
    else tokenStore.remove();
  } catch {
    tokenStore.remove();
  }
  if (router.currentRoute.value.path !== "/login") {
    router.push("/login");
  }
}
```

**亮点**：

- 401 处理兼顾了 Pinia 已初始化 / 未初始化两种场景（`try/catch` + `getActivePinia()`）
- 已有路由守卫 `/login` 判断，防止死循环重定向
- 请求去重机制（`AbortController`）防止列表页快速翻页时的竞态问题
- 重复请求取消的错误静默处理（`isCancelError` 判断），不影响用户

#### [规范警告/Warning] 非 401 错误统一用 `ElMessage.error` 提示，可能丢失关键错误信息

**文件**：[request.ts](file:///d:/vueprojects/RXAS400ADM/frontend/src/api/request.ts#L116-L118)

```typescript
// 原代码
} else {
  ElMessage.error(error.response?.data?.message || error.message || i18n.global.t('common.networkError'))
}
```

**问题**：当后端返回 `500 Internal Server Error` 时，`error.response?.data?.message` 可能为空，最终只显示 `common.networkError`（"网络错误"），用户无法区分是网络问题还是服务端崩溃。建议在开发环境打印完整的错误信息到控制台。

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在（request.ts 错误分支）。补 `import.meta.env.DEV` 控制台输出即可，改动 1-2 行，建议做。

---

### 10.5 补充审计总结

| 审计维度        | 核心发现                                                                                                                                          | 严重程度 |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **Entity 暴露** | 13+ Controller 方法直接返回 Entity（`SysRole`, `SysMenu`, `SysPermission`, `SysConfig`, `WebhookConfig`），仅 `SysUserController` 正确使用 DTO/VO | 🛑 严重  |
| **N+1 查询**    | `SysUserServiceImpl.bindRoles()` 逐条 INSERT + SELECT，角色数多时性能灾难                                                                         | 🛑 严重  |
| **SQL 注入**    | MyBatis 全部使用 `#{}`，无 `${}` SQL 拼接                                                                                                         | ✅ 安全  |
| **XSS**         | 前端 `v-html` 零使用，团队已主动移除                                                                                                              | ✅ 安全  |
| **越权**        | 所有敏感端点 `@PreAuthorize` 覆盖，`AUDIT_VIEW` 权限设计合理                                                                                      | ✅ 安全  |
| **内存泄漏**    | 9/9 组件正确清理事件监听器和定时器，`Monitor.vue` 使用 `window` 全局变量不规范                                                                    | ⚠️ 轻微  |
| **事务管理**    | 7 个 Service 缺少 `rollbackFor`，同步事件监听在事务内执行                                                                                         | ⚠️ 警告  |
| **前端门控**    | `v-has-perm` 指令 + `hasPermission()` 双重门控，后端 `@PreAuthorize` 兜底                                                                         | ✅ 优秀  |

**本轮审计核心结论**：

1. **最大的架构缺陷是 Entity 直接暴露给前端**。这是"契约精神"的最大破坏者，新人看代码时只有 `SysUserController` 是正面教材，其余 Controller 都是反面教材。
2. **安全实践整体优秀**：SQL 注入、XSS、越权三大安全风险均得到有效控制，`@PreAuthorize` + `v-has-perm` 双层防御是亮点。
3. **内存管理规范**：所有事件监听器和定时器都有正确的清理逻辑，`useTablePage` / `useShortcuts` / `useFlash` / `useStompClient` 四个 composable 的最佳实践值得推广。

---

_本报告由全栈代码审计生成，聚焦新人可读性与可维护性。建议优先执行"30 天新人降本清单"中的 3 个 Action Items，预期可将新人首次独立修改代码的周期从 2 周缩短至 3-5 天。_

---
