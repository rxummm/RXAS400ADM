# Dashboard 仪表盘

## 仪表盘加载流程

```mermaid
sequenceDiagram
    actor U as 用户
    participant Dashboard as Dashboard.vue
    participant Layout as Layout.vue
    participant Pinia as Pinia as400ServerStore
    participant Axios as Axios
    participant As400Ctrl as As400Controller
    participant MonitorCtrl as MonitorController
    participant MetricSvc as MetricService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>Layout: 进入 Dashboard
    Layout->>Dashboard: 挂载组件

    Note over Dashboard: 步骤1: 加载服务器列表
    Dashboard->>As400Ctrl: GET /api/v1/as400/servers
    As400Ctrl-->>Dashboard: 服务器列表 (IbmiSystemVO)

    Note over Dashboard: 步骤2: 用户选择服务器
    Dashboard->>Pinia: selectServer(serverId)
    Pinia->>Axios: 设置 X-AS400-Server 头

    Note over Dashboard: 步骤3: 加载概览指标
    Dashboard->>MonitorCtrl: GET /api/v1/monitor/overview/{id}
    MonitorCtrl->>MetricSvc: overview(serverId)
    MetricSvc->>AS400Client: JTOpenSqlClient → DB2 for i
    Note right of AS400Client: 🗄️ QSYS2.SYSTEM_STATUS_INFO<br/>CPU使用率 / 内存 / 磁盘
    AS400Client-->>MetricSvc: 指标数据
    MetricSvc-->>MonitorCtrl: Map(CPU, MEM, DISK, MSGW, LCKW)
    MonitorCtrl-->>Dashboard: 概览指标

    Note over Dashboard: 步骤4: 渲染仪表盘
    Dashboard->>Dashboard: ECharts 仪表盘 / 折线图 / 进度条
```

### 涉及组件

| 组件 | 路径 | 说明 |
|------|------|------|
| `Dashboard.vue` | `frontend/src/views/` | 仪表盘主页，ECharts 渲染 |
| `Layout.vue` | `frontend/src/layout/` | 主布局，侧边栏+顶栏 |
| `Pinia as400ServerStore` | `frontend/src/stores/` | 服务器选择状态管理 |
| `As400Controller` | `backend/.../as400/` | 服务器列表接口 |
| `MonitorController` | `backend/.../monitor/` | 监控指标接口 |

### 涉及数据表

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `rx_ibmi_system` | 📖 | 启用的服务器列表 |
| `QSYS2.SYSTEM_STATUS_INFO` | 🗄️ | CPU/内存/磁盘使用率 |
| `QSYS2.ACTIVE_JOB_INFO` | 🗄️ | MSGW/LCKW 作业计数 |
| `rx_metric` | 📖 | 最近采集指标快照 |