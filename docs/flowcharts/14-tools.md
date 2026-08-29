# 发布与代码 / 工具

## 工具模块 {#tools}

`/api/v1/audit-logs` `/api/v1/health` `/api/v1/topology` `/api/v1/regions` `/api/v1/calendar/events` `/api/v1/executions`

### 审计日志

```mermaid
sequenceDiagram
    actor U as 用户
    participant AuditView as audit/index.vue
    participant AuditCtrl as AuditLogController
    participant AuditSvc as IAuditLogService
    participant DB as 业务数据库

    U->>AuditView: 进入审计日志
    AuditView->>AuditCtrl: GET /api/v1/audit-logs?current=1&size=20&module=xxx&username=xxx&action=xxx&keyword=xxx<br/>🔒 AUDIT_VIEW
    AuditCtrl->>AuditSvc: page(current, size, module, username, action, keyword)
    Note right of AuditSvc: 📖 rx_audit_log<br/>按模块/用户/操作/时间过滤
    AuditSvc-->>AuditCtrl: PageResult<AuditLogVO>
    AuditCtrl-->>AuditView: 分页审计日志（含 ip / target / detail / createdTime）
```

### 健康巡检

```mermaid
sequenceDiagram
    actor U as 用户
    participant HealthView as health/index.vue
    participant HealthCtrl as HealthController
    participant HealthSvc as HealthService
    participant DB as 业务数据库
    participant AS400Client as AS400ClientProvider

    U->>HealthView: 进入健康巡检
    HealthView->>HealthCtrl: GET /api/v1/health<br/>🔒 HEALTH_VIEW
    HealthCtrl->>HealthSvc: reportWithServers()
    Note right of HealthSvc: 并行探测 + 30s 缓存<br/>检查 DB / Quartz 调度器 / 各服务器连接<br/>未关闭告警 / 指标采集情况
    HealthSvc->>DB: 检查数据库连通性
    HealthSvc->>AS400Client: 并行探测各 AS400 服务器
    AS400Client-->>HealthSvc: 各服务器连接状态
    HealthSvc-->>HealthCtrl: HealthReportVO
    HealthCtrl-->>HealthView: 一站式巡检报告（含各组件健康状态）
```

### 拓扑分析

```mermaid
sequenceDiagram
    actor U as 用户
    participant TopoView as topology/index.vue
    participant TopoCtrl as TopologyController
    participant TopoSvc as ITopologyService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>TopoView: 进入拓扑视图
    TopoView->>TopoCtrl: GET /api/v1/topology?library=APP<br/>🔒 TOPOLOGY_VIEW
    TopoCtrl->>TopoSvc: graph(library)
    Note right of TopoSvc: AS400Client → DB2 for i<br/>查询库级对象引用关系
    TopoSvc->>AS400Client: 查询对象依赖关系
    AS400Client->>IBMi: 查询对象引用
    IBMi-->>AS400Client: 对象引用数据
    AS400Client-->>TopoSvc: 引用关系数据
    TopoSvc-->>TopoCtrl: GraphData (nodes + links)
    TopoCtrl-->>TopoView: ECharts 关系图（点击节点可下钻对象详情）
```

### 区域管理

```mermaid
sequenceDiagram
    actor U as 管理员
    participant RegionView as tool/region/index.vue
    participant RegionCtrl as RegionController
    participant RegionSvc as IRegionService
    participant DB as 业务数据库

    U->>RegionView: 进入区域管理
    RegionView->>RegionCtrl: GET /api/v1/regions/children?parentCode=xxx<br/>🔒 REGION_VIEW
    Note right of RegionCtrl: IRegionService.children(parentCode)<br/>📖 rx_region<br/>懒加载子级树
    RegionCtrl-->>RegionView: 下级区域列表

    RegionView->>RegionCtrl: GET /api/v1/regions/page?current=1&size=15&keyword=xxx&level=xxx&parentCode=xxx<br/>🔒 REGION_VIEW
    RegionCtrl->>RegionSvc: page(current, size, keyword, level, parentCode)
    Note right of RegionSvc: 📖 rx_region
    RegionSvc-->>RegionCtrl: PageResult<RegionVO>
    RegionCtrl-->>RegionView: 区域分页列表

    RegionView->>RegionCtrl: GET /api/v1/regions/search?keyword=xxx&level=xxx<br/>🔒 REGION_VIEW
    RegionCtrl->>RegionSvc: search(keyword, level)
    RegionCtrl-->>RegionView: 搜索结果

    RegionView->>RegionCtrl: POST /api/v1/regions<br/>🔒 REGION_MANAGE<br/>@OperateLog("新增行政区划")
    Note right of RegionCtrl: Body: RegionDTO
    RegionCtrl->>RegionSvc: create(region)
    Note right of RegionSvc: ✏️ rx_region
    RegionSvc-->>RegionCtrl: Region
    RegionCtrl-->>RegionView: RegionVO

    RegionView->>RegionCtrl: PUT /api/v1/regions/{id}<br/>🔒 REGION_MANAGE<br/>@OperateLog("修改行政区划")
    RegionCtrl->>RegionSvc: update(id, dto)
    Note right of RegionSvc: ✏️ rx_region
    RegionSvc-->>RegionCtrl: Region
    RegionCtrl-->>RegionView: 更新成功

    RegionView->>RegionCtrl: DELETE /api/v1/regions/{id}<br/>🔒 REGION_MANAGE<br/>@OperateLog("删除行政区划")
    RegionCtrl->>RegionSvc: delete(id)
    Note right of RegionSvc: ✏️ rx_region
    RegionSvc-->>RegionCtrl: void
    RegionCtrl-->>RegionView: 200 OK
```

### 日历

```mermaid
sequenceDiagram
    actor U as 用户
    participant CalView as calendar/index.vue
    participant CalCtrl as CalendarController
    participant CalSvc as ICalendarEventService
    participant DB as 业务数据库

    U->>CalView: 进入日历
    CalView->>CalCtrl: GET /api/v1/calendar/events/month?year=2026&month=8<br/>🔒 CALENDAR_VIEW
    CalCtrl->>CalSvc: month(year, month, currentUserId)
    Note right of CalSvc: 📖 rx_calendar_event<br/>按创建人隔离
    CalSvc-->>CalCtrl: List<CalendarEvent> → List<CalendarEventVO>
    CalCtrl-->>CalView: 月视图事件列表

    CalView->>CalCtrl: POST /api/v1/calendar/events<br/>🔒 CALENDAR_MANAGE<br/>@OperateLog("新增事件")
    Note right of CalCtrl: Body: CalendarEventDTO<br/>{title, description, eventDate, eventTime, color}
    CalCtrl->>CalSvc: create(event, currentUserId)
    Note right of CalSvc: ✏️ rx_calendar_event
    CalSvc-->>CalCtrl: CalendarEvent
    CalCtrl-->>CalView: CalendarEventVO

    CalView->>CalCtrl: PUT /api/v1/calendar/events/{id}<br/>🔒 CALENDAR_MANAGE<br/>@OperateLog("修改事件")
    CalCtrl->>CalSvc: update(id, dto, currentUserId)
    Note right of CalSvc: ✏️ rx_calendar_event
    CalSvc-->>CalCtrl: CalendarEvent
    CalCtrl-->>CalView: 更新成功

    CalView->>CalCtrl: DELETE /api/v1/calendar/events/{id}<br/>🔒 CALENDAR_MANAGE<br/>@OperateLog("删除事件")
    CalCtrl->>CalSvc: delete(id, currentUserId)
    Note right of CalSvc: ✏️ rx_calendar_event
    CalSvc-->>CalCtrl: void
    CalCtrl-->>CalView: 200 OK
```

### 执行记录

```mermaid
sequenceDiagram
    actor U as 用户
    participant ExecView as executions/index.vue
    participant ExecCtrl as ExecutionController
    participant ExecSvc as ExecutionService
    participant DB as 业务数据库

    U->>ExecView: 进入执行记录
    ExecView->>ExecCtrl: GET /api/v1/executions?type=xxx&status=xxx&keyword=xxx&current=1&size=20<br/>🔒 EXECUTION_VIEW
    ExecCtrl->>ExecSvc: scheduleExecutions(status, keyword, capped)<br/>scriptExecutions(status, keyword, capped)
    Note right of ExecSvc: 📖 rx_schedule_execution<br/>📖 rx_script_execution<br/>合并两类执行记录，按 runTime 倒序
    ExecSvc-->>ExecCtrl: merged list<Map> → PageResult<ExecutionRecordVO>
    ExecCtrl-->>ExecView: 执行记录分页列表

    ExecView->>ExecCtrl: GET /api/v1/executions/stats<br/>🔒 EXECUTION_VIEW
    ExecCtrl->>ExecSvc: getStats()
    Note right of ExecSvc: 统计 totalExecutions / successRate<br/>successCount / failedCount
    ExecSvc-->>ExecCtrl: ExecutionStatsVO
    ExecCtrl-->>ExecView: 统计卡片（总执行数/成功率/成功数/失败数）
```

## 发布与代码 {#release}

`/api/v1/source/*`

### 源代码浏览

```mermaid
sequenceDiagram
    actor U as 用户
    participant SourceView as Source.vue
    participant SourceCtrl as SourceController
    participant SourceSvc as ISourceService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>SourceView: 进入源代码
    SourceView->>SourceCtrl: GET /api/v1/source/libraries<br/>🔒 SOURCE_VIEW
    SourceCtrl->>SourceSvc: listLibraries()
    Note right of SourceSvc: AS400Client → QSYS2.SYSPARTITIONSTAT<br/>或 DLIB 列表
    SourceSvc->>AS400Client: 查询源码库列表
    AS400Client->>IBMi: 获取库列表
    IBMi-->>AS400Client: 库列表
    AS400Client-->>SourceSvc: 库名列表
    SourceSvc-->>SourceCtrl: List<String>
    SourceCtrl-->>SourceView: 源码库下拉

    U->>SourceView: 选择库 → 查看源文件
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{library}/files<br/>🔒 SOURCE_VIEW
    SourceCtrl->>SourceSvc: listSourceFiles(library)
    Note right of SourceSvc: AS400Client → QSYS2.SYSPARTITIONSTAT<br/>列出源物理文件
    SourceSvc->>AS400Client: 查询源文件列表
    AS400Client->>IBMi: 获取源文件列表
    IBMi-->>AS400Client: 源文件列表
    AS400Client-->>SourceSvc: 文件名列表
    SourceSvc-->>SourceCtrl: List<String>
    SourceCtrl-->>SourceView: 源文件列表

    U->>SourceView: 选择源文件 → 查看成员
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{library}/files/{file}/members<br/>🔒 SOURCE_VIEW
    SourceCtrl->>SourceSvc: listMembers(library, file)
    Note right of SourceSvc: AS400Client → DSPFD 或 QSYS2.SYSPARTITIONSTAT
    SourceSvc->>AS400Client: 查询成员列表
    AS400Client->>IBMi: 获取成员列表
    IBMi-->>AS400Client: 成员列表
    AS400Client-->>SourceSvc: 成员名列表
    SourceSvc-->>SourceCtrl: List<String>
    SourceCtrl-->>SourceView: 源码成员列表

    U->>SourceView: 点击成员查看源码
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{library}/files/{file}/members/{member}<br/>🔒 SOURCE_VIEW
    SourceCtrl->>SourceSvc: readMember(library, file, member)
    Note right of SourceSvc: AS400Client → 读取源码内容<br/>RPGLE / SQLRPGLE / CLP / CMD
    SourceSvc->>AS400Client: 读取源码成员
    AS400Client->>IBMi: 获取源码内容
    IBMi-->>AS400Client: 源码文本
    AS400Client-->>SourceSvc: 源码内容
    SourceSvc-->>SourceCtrl: Map<String, String>（含源码文本 + 属性）
    SourceCtrl-->>SourceView: 源码内容 + 语法高亮
```

### 涉及功能模块

| 模块 | 路由 | 前端组件 | 后端 Controller | Service |
|------|------|----------|-----------------|---------|
| 审计日志 | `/audit` | `audit/index.vue` | `AuditLogController` | `IAuditLogService` |
| 健康巡检 | `/health` | `health/index.vue` | `HealthController` | `HealthService` |
| 拓扑分析 | `/topology` | `topology/index.vue` | `TopologyController` | `ITopologyService` |
| 区域管理 | `/region` | `tool/region/index.vue` | `RegionController` | `IRegionService` |
| 日历 | `/calendar` | `calendar/index.vue` | `CalendarController` | `ICalendarEventService` |
| 执行记录 | `/executions` | `executions/index.vue` | `ExecutionController` | `ExecutionService` |
| 源代码 | `/source` | `Source.vue` | `SourceController` | `ISourceService` |

### 涉及数据表

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `rx_audit_log` | 📖 | 审计日志（@OperateLog 注解自动写入） |
| `rx_region` | 📖✏️ | 行政区划（省/市/区/街道 四级树） |
| `rx_calendar_event` | 📖✏️ | 日历事件（按创建人隔离） |
| `rx_schedule_execution` | 📖 | 定时任务执行记录（执行记录合并数据源） |
| `rx_script_execution` | 📖 | 命令脚本执行记录（执行记录合并数据源） |

### 涉及权限码

| 权限码 | 说明 | 使用位置 |
|--------|------|---------|
| `AUDIT_VIEW` | 审计查看 | 审计日志分页查询 |
| `HEALTH_VIEW` | 健康巡检查看 | 健康巡检报告 |
| `TOPOLOGY_VIEW` | 拓扑查看 | 拓扑关系图查询 |
| `REGION_VIEW` | 区域查看 | 区域树/分页/搜索 |
| `REGION_MANAGE` | 区域管理 | 区域 CRUD |
| `CALENDAR_VIEW` | 日历查看 | 日历月视图 |
| `CALENDAR_MANAGE` | 日历管理 | 日历事件 CRUD |
| `EXECUTION_VIEW` | 执行记录查看 | 执行记录分页/统计 |
| `SOURCE_VIEW` | 源码查看 | 源码库/文件/成员/内容浏览 |