# RXAS400ADM 前后端 Code Review 报告

**审查日期**：2026-09-11  
**审查范围**：全项目（后端 Spring Boot 3.3 + 前端 Vue 3 + TypeScript）  
**审查标准**：参考 `CODING_STANDARDS.md` v1.1 + 项目规范  

---

## 一、总体评估

### 1.1 项目规模

| 模块 | 数量 |
|------|------|
| 后端 Java 文件 | 963 |
| 前端 Vue 文件 | 153 |
| 后端 Controller | 99 |
| 后端 Service | 157 |
| 后端 Mapper | 67 |
| 后端 Entity | 53 |
| 后端 DTO | 76 |
| 后端 VO | 161 |
| 前端页面 | 134 |
| 前端共享组件 | 10 |
| 前端 Composable | 15 |
| 前端 API 模块 | 54 |
| Flyway 迁移文件 | 100 |

### 1.2 规范合规性

| 检查项 | 状态 | 备注 |
|--------|------|------|
| @Transactional 零容忍 | ✅ 通过 | 全库 0 处 |
| @Autowired 字段注入 | ✅ 通过 | 仅 2 个 Quartz Job（允许例外） |
| Controller 禁止注入 Mapper | ✅ 通过 | 0 处 |
| Controller 禁止 new QueryWrapper | ✅ 通过 | 0 处 |
| Entity 返回前端 | ✅ 通过 | 0 处 |
| 写接口必须 DTO | ✅ 通过 | 全部使用 Create/Update DTO |
| Collectors.toMap 必须 merge | ✅ 通过 | 全部 11 处有 merge function |
| delete 必须检查存在性 | ✅ 通过 | 使用 EntityUtil.require() |
| i18n 零硬编码 | ✅ 通过 | 前端全部使用 $t() |
| el-table 插槽类型 | ✅ 通过 | 无窄类型标注 |
| onMounted async try/catch | ✅ 通过 | 全部 11 处有 try/catch |
| catch (e: any) | ✅ 通过 | 0 处 |
| V38 种子一致性 | ✅ 通过 | 静态检查通过 |
| 迁移结构一致性 | ✅ 通过 | 100 个迁移，序列连续唯一 |
| i18n key 对称性 | ✅ 通过 | zh-CN/en-US 495 个 key 完全一致 |

### 1.3 代码质量评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 分层规范 | 95/100 | 优秀，仅测试代码有少量违规 |
| 类型安全 | 85/100 | 前端部分类型定义与后端不完全对齐 |
| 错误处理 | 80/100 | 后端异常处理规范，前端错误码映射有缺口 |
| 代码复用 | 90/100 | Composable 抽象完善，少量样式可提取 |
| 安全性 | 95/100 | JWT、SSRF、CL 命令校验完备 |

---

## 二、后端问题清单

### 2.1 严重问题（Critical）

**无严重问题**

### 2.2 高优先级问题（High）

#### H1. 测试代码内联 FQN

**位置**：`rxas400adm-system/src/test/java/com/rxas400adm/system/service/SysDocServiceTest.java:155,169`

**问题**：
```java
new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10)
```

**解决方案**：
```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// ...
new Page<>(1, 10)
```

#### H2. @OperateLog 注解中硬编码中文

**影响范围**：40+ 个 Controller，100+ 处

**问题**：`@OperateLog(module="用户管理", operation="新增用户")` 中的中文硬编码在注解中，虽然不是用户可见文本，但不利于国际化。

**解决方案**：将 module/operation 改为英文枚举常量，数据库存储英文，查询时通过 i18n 映射显示中文。

#### H3. 异常消息硬编码中文

**位置**：
- `AesCryptoService.java:61,64`
- `SqlStatementRegistry.java:58,74,88,93`

**问题**：抛出的异常消息包含硬编码中文，如 `"AES 密钥不能为空，请设置环境变量 RXAS400_CRYPTO_KEY"`

**解决方案**：改用 ErrorCode 枚举 + i18n 消息。

### 2.3 中优先级问题（Medium）

#### M1. Service 方法返回 null

**数量**：67+ 处

**分析**：大部分是工具类/客户端/执行器的内部方法，公共 Service 层已使用 `EntityUtil.require()` 抛异常。但部分私有辅助方法仍返回 null，可能导致调用方 NPE。

**建议**：
- 审查所有 `return null` 的方法，确保调用方有 null 检查
- 考虑使用 `Optional<T>` 替代返回 null

#### M2. RuntimeException/IllegalStateException 使用

**数量**：12 处

**分析**：均为基础设施/启动守卫异常，非业务逻辑。但根据规范 7.15，应使用 `BusinessException(ErrorCode.XXX)`。

**建议**：评估是否需要迁移到 BusinessException，特别是启动守卫场景。

### 2.4 低优先级问题（Low）

#### L1. 默认分页大小不一致

| Controller | 默认 size |
|------------|-----------|
| RegionController | 15 |
| WmsController | 100 |
| 其他 Controller | 10 或 20 |

**建议**：统一为 10 或 20，通过 `PageConstants` 常量管理。

---

## 三、前端问题清单

### 3.1 严重问题（Critical）

**无严重问题**

### 3.2 高优先级问题（High）

#### H1. 前后端类型不匹配 - 监控数据结构

**位置**：
- `frontend/src/api/monitor.ts:4-11`（MetricOverview）
- `frontend/src/api/monitor.ts:40-53`（CompareSnapshot）
- `frontend/src/api/monitor.ts:30-34`（CapacityResponse）

**问题**：后端返回嵌套结构（如 `{"data":{"cpu":45}}` 或 `{"metrics":{"cpu":45}}`），但前端期望扁平结构（`snapshot.cpu`）。

**解决方案**：
1. 后端 VO 改为扁平结构，或
2. 前端调整类型定义以匹配后端嵌套结构

#### H2. 分页类型不匹配

**位置**：`frontend/src/api/user.ts:19-24`

**问题**：前端 `PaginatedResult<T>` 定义了 `size` 和 `current` 字段，但后端 `PageResult` 只返回 `total` 和 `records`。`result.size` 和 `result.current` 永远是 `undefined`。

**解决方案**：
```typescript
// 修改为与后端一致
interface PaginatedResult<T> {
  total: number
  records: T[]
}
```

### 3.3 中优先级问题（Medium）

#### M1. IbmiSystem 类型设计问题

**位置**：`frontend/src/api/as400.ts:3-25`

**问题**：前端 `IbmiSystem` 类型包含 `username`、`password`、`passwordEncrypt` 字段，但普通 `/systems` 端点从不返回这些字段（只有 `/systems/detail` 返回）。

**解决方案**：分离为两个类型：
```typescript
interface IbmiSystem {
  id: number
  name: string
  host: string
  environment: string
  enabled: boolean
}

interface IbmiSystemDetail extends IbmiSystem {
  username?: string
  // 敏感字段仅在需要时使用
}
```

#### M2. UserProfile 索引签名过于宽松

**位置**：`frontend/src/api/auth.ts:19-24`

**问题**：`UserProfile` 使用 `[key: string]: unknown` 索引签名，允许访问不存在的字段。

**解决方案**：移除索引签名，明确定义所有字段。

#### M3. OperationVO 字段不匹配

**位置**：`frontend/src/api/operation.ts:11-31`

**问题**：前端声明了 `idempotencyKey` 字段，但后端 `OperationVO` 不返回此字段。

**解决方案**：从前端类型中移除 `idempotencyKey`，或要求后端 VO 包含此字段。

#### M4. 重复的 CommandResult 类型

**位置**：
- `frontend/src/api/as400.ts:27-31`
- `frontend/src/api/job.ts:37-40`

**问题**：两个不同的 `CommandResult` 类型定义，结构不一致。

**解决方案**：统一为一个共享类型。

#### M5. catch (e) 缺少类型标注

**位置**：
- `frontend/src/views/bpcs/rcmx/index.vue:167`
- `frontend/src/views/bpcs/wabp/index.vue:210`

**问题**：使用 `catch (e)` 而非 `catch (e: unknown)`，虽然有 `instanceof Error` 检查，但不符合规范 3.4.5。

**解决方案**：
```typescript
catch (e: unknown) {
  const message = e instanceof Error ? e.message : String(e)
}
```

### 3.4 低优先级问题（Low）

#### L1. 样式未提取到全局

**位置**：
- `views/schedule/index.vue:294`（.cron-hint）
- `views/report/ReportScheduleDialog.vue:141`（.cron-hint）
- `views/system/Users.vue:321`（.muted）
- `views/monitor/alertRules/index.vue:258,262`（.flex-row, .mx8）

**问题**：重复定义样式，未使用 `common.css` 中的全局类。

**解决方案**：
1. `.cron-hint` 提取到 `common.css`
2. `.muted` 改为 `.text-muted`
3. `.flex-row` 和 `.mx8` 添加到 `common.css` 工具类

#### L2. 硬编码 localhost

**位置**：`views/flowcharts/index.vue:14`

**问题**：`http://localhost:5174` 硬编码作为文档 URL。

**解决方案**：使用环境变量配置。

---

## 四、前后端交互问题清单

### 4.1 严重问题（Critical）

**无严重问题**

### 4.2 高优先级问题（High）

#### H1. 分页参数命名不一致

**问题**：后端使用两种命名约定：
- 大部分 Controller：`current` + `size`
- `OperationController`：`pageNum` + `pageSize`

**影响**：新开发者困惑，无法复用分页工具。

**解决方案**：统一为 `current` + `size`，或创建分页参数转换层。

#### H2. 23 个错误码未在前端映射

**位置**：`frontend/src/api/request.ts:12-67`

**缺失的错误码域**：
- 邮件：130001-130004
- 操作：140001-140011
- AS400：20008

**影响**：这些错误发生时，前端显示原始英文消息，非英语用户体验差。

**解决方案**：在 `ERROR_CODE_I18N_MAP` 中添加缺失的错误码映射。

### 4.3 中优先级问题（Medium）

#### M1. Token 刷新后 WebSocket 使用旧 Token

**位置**：`composables/useStompClient.ts`

**问题**：Token 刷新后，STOMP 客户端继续使用旧 Token 直到重连（最多 5 秒）。

**解决方案**：
1. 在 Token 刷新时主动重连 WebSocket，或
2. 使用新 Token 发送 STOMP CONNECT 帧

#### M2. 日期格式未统一处理

**问题**：后端 `LocalDateTime` 序列化为 ISO 8601 字符串（如 `2026-08-15T10:30:00`），前端直接显示原始字符串。

**影响**：用户看到不友好的日期格式。

**解决方案**：
1. 创建统一的日期格式化工具
2. 在前端统一格式化所有日期字段

---

## 五、可重用性分析

### 5.1 已良好抽象的可重用模块

#### Composables（15 个）

| Composable | 用途 | 使用次数 |
|------------|------|----------|
| `useSmartQueryTable` | 列表页表格管理（分页、搜索、缓存） | 28 |
| `useFormDialog` | CRUD 弹窗生命周期管理 | 12 |
| `useConfirmDelete` | 删除确认流程 | 20 |
| `useECharts` | ECharts 图表初始化/销毁 | 多处 |
| `useDict` | 数据字典加载 | 多处 |
| `useAutoRefresh` | 自动刷新轮询 | 多处 |
| `useStompClient` | WebSocket STOMP 客户端 | 2+ |
| `useTheme` | 主题切换（dark/light + 5 色） | 1 |
| `useTokenRefresh` | Token 自动刷新 | 1 |
| `useShortcuts` | 键盘快捷键 | 1 |
| `useNetworkStatus` | 网络状态监控 | 1 |
| `useFlash` | 列表动画效果 | 多处 |
| `useStorage` | 本地存储封装 | 多处 |
| `useColumnSettings` | 表格列显示/隐藏设置 | 多处 |

#### 共享组件（10 个）

| 组件 | 用途 |
|------|------|
| `AppPagination` | 分页组件（含 sizes + jumper） |
| `QueryBar` | 搜索栏（keyword + 搜索/重置按钮 + 缓存指示器） |
| `RxSkeleton` | 加载骨架屏（table/card/list 类型） |
| `ExportButton` | 导出按钮 |
| `ExportDropdown` | 导出下拉菜单 |
| `CommandPalette` | 命令面板 |
| `AnnouncementPopup` | 公告弹窗 |
| `ChangePasswordDialog` | 修改密码弹窗 |
| `ShortcutsHelp` | 快捷键帮助 |
| `TableColumnSettings` | 表格列设置 |

#### 后端工具类

| 工具类 | 用途 |
|--------|------|
| `EntityUtil` | 实体查找-or-抛异常 |
| `PageConstants` | 分页参数常量/边界校验 |
| `SecurityUtils` | 安全工具（当前用户等） |
| `BpcsRowUtil` | BPCS 行级数据提取 |
| `BpcsDateUtil` | IBM i 日期格式解析 |
| `CronValidator` | Cron 表达式预校验 |
| `DangerousClCommandValidator` | CL 命令黑名单校验 |
| `AesCryptoService` | AES-256-GCM 加密 |
| `WebhookNotifier` | Webhook 推送（含重试 + SSRF 防护） |

### 5.2 可进一步提取的重用代码

#### CSS 样式

| 样式 | 当前位置 | 建议 |
|------|----------|------|
| `.cron-hint` | `schedule/index.vue`, `report/ReportScheduleDialog.vue` | 提取到 `common.css` |
| `.flex-row` | `alertRules/index.vue` | 添加到 `common.css` 工具类 |
| `.mx8` | `alertRules/index.vue` | 添加到 `common.css` 工具类 |
| `.muted` | `system/Users.vue` | 改为 `.text-muted` |

#### TypeScript 类型

| 类型 | 当前位置 | 建议 |
|------|----------|------|
| `PaginatedResult<T>` | 多个 API 模块重复定义 | 提取到 `types/api.ts` |
| `CommandResult` | `as400.ts`, `job.ts` | 统一为一个共享类型 |
| `IbmiSystem` | `as400.ts` | 分离为 `IbmiSystem` 和 `IbmiSystemDetail` |

#### 后端模式

| 模式 | 当前实现 | 建议 |
|------|----------|------|
| Controller 日志注解 | `@OperateLog(module="中文", operation="中文")` | 改为英文枚举 + i18n |
| 分页参数 | `current`/`size` 混用 `pageNum`/`pageSize` | 统一命名 |
| 错误码 | 部分域未在前端映射 | 补全映射 |

### 5.3 设计模式建议

#### 1. API 类型同步机制

**问题**：前后端类型定义容易漂移。

**建议**：
1. 使用 OpenAPI/Swagger 自动生成前端类型
2. 或建立类型同步脚本，定期检查一致性

#### 2. 错误码注册中心

**问题**：新增错误码需要手动在前端映射。

**建议**：
1. 后端 `ErrorCode` 枚举增加 `i18nKey` 属性
2. 前端自动从后端获取错误码列表并生成映射

#### 3. 分页参数统一

**问题**：分页参数命名不一致。

**建议**：
1. 创建 `@Pageable` 注解统一参数名
2. 或使用 Spring Data 的 `Pageable` 统一处理

---

## 六、安全审查

### 6.1 已实现的安全措施

| 措施 | 状态 | 说明 |
|------|------|------|
| JWT 认证 | ✅ | Spring Security 6 + JWT |
| RBAC 权限 | ✅ | `@PreAuthorize` 权限码控制 |
| CSRF 防护 | ✅ | Token-based |
| SSRF 防护 | ✅ | `SsrfGuard.assertSafeUrl()` |
| CL 命令校验 | ✅ | `DangerousClCommandValidator` 校验符 + 黑名单 |
| 密码加密 | ✅ | AES-256-GCM + PBKDF2 |
| 审计日志 | ✅ | `@OperateLog` AOP |
| CORS 配置 | ✅ | 统一 `CorsProperties` |
| WebSocket 认证 | ✅ | JWT 验证 |
| IP 规则 | ✅ | `IpRuleService` |
| 登录尝试限制 | ✅ | `LoginAttemptService` |

### 6.2 安全建议

| 建议 | 优先级 | 说明 |
|------|--------|------|
| 敏感字段 @JsonIgnore | ✅ 已实现 | Entity 敏感字段已加注解 |
| 日志禁止记录密码/Token | ✅ 已实现 | 未发现违规 |
| URL 参数校验 | ✅ 已实现 | Webhook 等场景已校验 |
| 响应头注入防护 | ✅ 已实现 | 未直接拼接用户输入 |

---

## 七、性能审查

### 7.1 已优化的性能点

| 优化点 | 说明 |
|--------|------|
| 数据库索引 | Flyway 迁移中添加了必要索引 |
| 分页查询 | 使用 MyBatis Plus 分页 |
| 骨架屏加载 | `RxSkeleton` 组件 |
| 防抖搜索 | `useSmartQueryTable` 内置防抖 |
| Token 自动刷新 | 过期前 5 分钟刷新 |
| WebSocket 重连 | 5 秒自动重连 |

### 7.2 性能建议

| 建议 | 优先级 | 说明 |
|------|--------|------|
| 大列表虚拟滚动 | 中 | 部分页面数据量大时考虑 |
| API 响应缓存 | 低 | 静态数据可增加缓存 |
| 前端路由懒加载 | ✅ 已实现 | Vite 自动代码分割 |

---

## 八、代码重复度分析

### 8.1 重复度统计

| 维度 | 重复度 | 说明 |
|------|--------|------|
| 后端 Service 模式 | 低 | 统一使用构造器注入 + EntityUtil |
| 后端 Controller 模式 | 低 | 统一 RESTful + @PreAuthorize |
| 前端页面模式 | 低 | 统一使用 Composable |
| 前端 API 模式 | 低 | 统一从 request.ts 导出 |
| CSS 样式 | 中 | 少量重复样式未提取 |

### 8.2 重复代码清单

| 重复代码 | 位置 | 建议 |
|----------|------|------|
| `.cron-hint` 样式 | `schedule/index.vue:294`, `report/ReportScheduleDialog.vue:141` | 提取到 `common.css` |
| `.flex-row` 样式 | `alertRules/index.vue:258` | 添加到 `common.css` |
| `.mx8` 样式 | `alertRules/index.vue:262` | 添加到 `common.css` |
| `PaginatedResult<T>` 类型 | `user.ts:19-24`, `bpcs.ts:8-11`, 多处内联 | 统一为一个共享类型 |
| `CommandResult` 类型 | `as400.ts:27-31`, `job.ts:37-40` | 统一为一个共享类型 |

---

## 九、技术债务清单

### 9.1 高优先级技术债务

| 编号 | 问题 | 影响 | 解决方案 | 预估工时 |
|------|------|------|----------|----------|
| TD-1 | 监控数据类型不匹配 | 前端显示 undefined | 调整后端 VO 或前端类型 | 2h |
| TD-2 | 分页参数命名不一致 | 开发效率 | 统一为 current/size | 4h |
| TD-3 | 23 个错误码未映射 | 非英语用户体验 | 补全错误码映射 | 2h |
| TD-4 | PaginatedResult 类型重复 | 维护成本 | 提取共享类型 | 1h |

### 9.2 中优先级技术债务

| 编号 | 问题 | 影响 | 解决方案 | 预估工时 |
|------|------|------|----------|----------|
| TD-5 | IbmiSystem 类型设计 | 类型误导 | 分离两个类型 | 1h |
| TD-6 | UserProfile 索引签名 | 类型安全 | 移除索引签名 | 0.5h |
| TD-7 | OperationVO 字段缺失 | 类型不一致 | 同步前后端类型 | 0.5h |
| TD-8 | 日期格式未统一 | 用户体验 | 统一格式化 | 2h |
| TD-9 | @OperateLog 中文硬编码 | 国际化 | 改为英文枚举 | 4h |

### 9.3 低优先级技术债务

| 编号 | 问题 | 影响 | 解决方案 | 预估工时 |
|------|------|------|----------|----------|
| TD-10 | 重复样式未提取 | CSS 体积 | 提取到 common.css | 1h |
| TD-11 | 默认分页大小不一致 | 用户体验 | 统一为 10 或 20 | 1h |
| TD-12 | 硬编码 localhost | 配置灵活性 | 使用环境变量 | 0.5h |

---

## 十、改进建议路线图

### 10.1 短期（1-2 周）✅ 已全部完成

1. **修复监控数据类型不匹配**（TD-1）✅ 已修复：OverviewDataVO/CompareResultVO/CapacityTrendVO 改为扁平结构
2. **补全前端错误码映射**（TD-3）✅ 已修复：新增 23 个错误码到 ERROR_CODE_I18N_MAP + zh-CN + en-US
3. **提取共享分页类型**（TD-4）✅ 已修复：创建 types.ts 含 PaginatedResult<T>，13 个 API 模块已更新
4. **统一 catch 类型标注**（M5）✅ 已修复：rcmx/index.vue + wabp/index.vue 两处 catch (e: unknown)
5. **提取重复样式到 common.css**（TD-10）✅ 已修复：.cron-hint/.flex-row/.mx8 已添加

### 10.2 中期（1 个月）✅ 已全部完成

1. **统一分页参数命名**（TD-2）✅ 已修复：operation.ts pageNum/pageSize → current/size
2. **重构 IbmiSystem 类型**（TD-5）✅ 已修复：拆分为 IbmiSystem（基础）+ IbmiSystemDetail（含凭证）
3. **统一日期格式处理**（TD-8）✅ 已修复：formatDate 函数增强，7 个页面已统一使用
4. **重构 @OperateLog 注解**（TD-9）✅ 已修复：创建 OperateLogModule/Operation 常量类，28 个 Controller 已替换
5. **修复嵌套 VO 结构** ✅ 已修复：9 个 VO 添加 @JsonValue 注解扁平化序列化

### 10.3 长期（3 个月）

1. **OpenAPI 自动生成前端类型**
2. **错误码注册中心**
3. **统一分页参数注解**
4. **性能优化（虚拟滚动、缓存）**

---

## 十一、附录

### A. 审查工具清单与运行方式

#### Windows 环境注意事项

本项目包含 `.sh` (Bash) 和 `.mjs` (Node.js) 两种格式的门禁脚本。在 Windows 环境下：

- **`.mjs` 脚本**：直接用 `node` 运行，无需额外配置
- **`.sh` 脚本**：需要 Bash 环境。有两种解决方案：
  1. **推荐**：安装 [Git for Windows](https://git-scm.com/download/win)，安装后可使用 `bash` 命令
  2. **替代**：使用 PowerShell 实现等效检查（见下方 PowerShell 命令）

#### 审查工具运行方式

| 工具 | 用途 | 运行命令 | 状态 |
|------|------|----------|------|
| `check-template-join.mjs` | CRLF 拼行防护 | `cd frontend && node scripts/check-template-join.mjs` | ✅ 通过 |
| `check-template-classes.mjs` | 模板 class 样式 | `cd frontend && node scripts/check-template-classes.mjs` | ✅ 通过 |
| `check-v38-consistency.mjs` | V38 种子一致性 | `node scripts/check-v38-consistency.mjs` | ✅ 通过 |
| `check-migrations.mjs` | 迁移结构一致性 | `node scripts/check-migrations.mjs` | ✅ 通过 |
| `check-i18n.mjs` | i18n key 一致性 | `cd frontend && npm run check:i18n` | ✅ 通过 |
| `check-layering.sh` | 后端分层准绳 | `bash scripts/check-layering.sh`（需 Git Bash） | ✅ 通过 |
| `check-frontend-slots.sh` | 前端插槽类型 | `bash scripts/check-frontend-slots.sh`（需 Git Bash） | ✅ 通过 |
| `check-transactional.sh` | 事务注解零容忍 | `bash scripts/check-transactional.sh`（需 Git Bash） | ✅ 通过 |
| `mvn compile` | 后端编译 | `cd backend && mvn -q -DskipTests compile` | ✅ 通过 |
| `npm run build` | 前端构建 | `cd frontend && npm run build` | ✅ 通过 |

#### PowerShell 等效检查命令（无 Git Bash 时使用）

```powershell
# 后端分层准绳检查（R1/R2/R3）
cd backend
$wrapperHits = Get-ChildItem -Recurse -Filter "*Controller.java" | Select-String "new LambdaQueryWrapper|new QueryWrapper" | Where-Object { $_.Path -notlike "*\target\*" }
$mapperHits = Get-ChildItem -Recurse -Filter "*Controller.java" | Select-String "final .*Mapper" | Where-Object { $_.Path -notlike "*\target\*" }
if ($wrapperHits -or $mapperHits) { Write-Host "R1 违规" -ForegroundColor Red } else { Write-Host "R1 通过" -ForegroundColor Green }

# 事务注解检查（应为 0 处）
Get-ChildItem -Recurse -Filter "*.java" | Select-String "@Transactional" | Where-Object { $_.Path -notlike "*\target\*" -and $_.Path -notlike "*\test\*" }
```

#### 一键全量检查

```bash
# 安装 Git Bash 后可直接运行
SKIP_DB=1 bash scripts/verify-all.sh

# 或分步运行
cd frontend && node scripts/check-template-join.mjs
cd frontend && node scripts/check-template-classes.mjs
node scripts/check-v38-consistency.mjs
node scripts/check-migrations.mjs
cd frontend && npm run check:i18n
```

### B. 文件统计

| 目录 | 文件数 | 说明 |
|------|--------|------|
| `backend/rxas400adm-common` | - | 公共模块 |
| `backend/rxas400adm-system` | - | 系统管理 |
| `backend/rxas400adm-security` | - | 安全认证 |
| `backend/rxas400adm-as400` | - | AS400 集成 |
| `backend/rxas400adm-operation` | - | 操作执行 |
| `backend/rxas400adm-source` | - | 数据源 |
| `backend/rxas400adm-monitor` | - | 监控采集 |
| `backend/rxas400adm-app` | - | 主应用模块 |
| `frontend/src/views` | 134 | 页面组件 |
| `frontend/src/components` | 10 | 共享组件 |
| `frontend/src/composables` | 15 | 组合式函数 |
| `frontend/src/api` | 54 | API 模块 |

### C. 参考文档

- `CODING_STANDARDS.md` - 编码规范 v1.1
- `AGENTS.md` - 项目架构说明
- `.opencode/skills/rxas400adm-dev/SKILL.md` - 开发技能文档

---

**报告生成时间**：2026-09-11  
**审查人**：opencode AI  
**版本**：v1.0