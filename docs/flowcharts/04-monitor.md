# 监控中心

## 4.1 监控概览与指标历史 {#metrics}

```mermaid
sequenceDiagram
    actor U as 用户
    participant Monitor as Monitor.vue
    participant MonitorCtrl as MonitorController
    participant MetricSvc as MetricService
    participant CapacitySvc as CapacityService
    participant BaselineSvc as BaselineService
    participant AlertSvc as AlertEventService
    participant DB as 业务数据库

    Note over U: 概览指标
    U->>Monitor: 选择服务器 → 查看概览
    Monitor->>MonitorCtrl: GET /api/v1/monitor/overview/{id}<br/>🔒 MONITOR_VIEW
    MonitorCtrl->>MetricSvc: overview(serverId)
    Note right of MetricSvc: 📖 rx_metric<br/>最近采集的 CPU/MEM/DISK/MSGW/LCKW
    MonitorCtrl-->>Monitor: 概览数据

    Note over U: 指标历史
    Monitor->>MonitorCtrl: GET /api/v1/monitor/metrics/{id}?limit=50<br/>🔒 MONITOR_VIEW
    MonitorCtrl->>MetricSvc: history(serverId, limit)
    Note right of MetricSvc: 📖 rx_metric<br/>时间序列查询
    MonitorCtrl-->>Monitor: 指标历史列表

    Note over U: 容量规划
    Monitor->>MonitorCtrl: GET /api/v1/monitor/capacity?instanceId={id}&days=30<br/>🔒 MONITOR_VIEW
    MonitorCtrl->>CapacitySvc: trend(instanceId, days)
    Note right of CapacitySvc: 📖 rx_metric<br/>DISK 趋势 + 线性回归预测
    MonitorCtrl-->>Monitor: 容量趋势图

    Note over U: 性能基线
    Monitor->>MonitorCtrl: GET /api/v1/monitor/baseline/{id}<br/>🔒 MONITOR_VIEW
    MonitorCtrl->>BaselineSvc: computeBaseline(id) + baselineWithCurrent(id)
    Note right of BaselineSvc: 📖 rx_metric<br/>基线计算 + 当前值偏差
    MonitorCtrl-->>Monitor: 基线对比数据
```

---

## 4.2 服务器对比与告警 {#server-compare}

```mermaid
sequenceDiagram
    actor U as 用户
    participant MonitorCtrl as MonitorController
    participant MetricSvc as MetricService
    participant SystemSvc as IbmiSystemService
    participant AlertSvc as AlertEventService
    participant DB as 业务数据库

    Note over U: 服务器对比
    U->>MonitorCtrl: GET /api/v1/monitor/compare?ids=1,2,3<br/>🔒 MONITOR_VIEW (最多20台)
    MonitorCtrl->>SystemSvc: get(id) × N
    Note right of SystemSvc: 📖 rx_ibmi_system
    MonitorCtrl->>MetricSvc: overview(id) × N
    Note right of MetricSvc: 📖 rx_metric<br/>当前指标快照
    MonitorCtrl-->>U: 多服务器并排对比

    Note over U: 告警事件
    U->>MonitorCtrl: GET /api/v1/monitor/alerts?limit=50<br/>🔒 MONITOR_VIEW
    MonitorCtrl->>AlertSvc: recent(limit)
    Note right of AlertSvc: 📖 rx_alert_event<br/>按创建时间倒序
    MonitorCtrl-->>U: 告警事件列表
```

---

## 4.3 告警规则管理 {#alert-rules}

```mermaid
sequenceDiagram
    actor U as 管理员
    participant AlertRuleCtrl as AlertRuleController
    participant AlertRuleSvc as AlertRuleService
    participant DB as 业务数据库

    U->>AlertRuleCtrl: GET /api/v1/alert-rules 🔒 MONITOR_VIEW
    Note right of AlertRuleSvc: 📖 rx_alert_rule
    AlertRuleCtrl-->>U: 告警规则列表

    U->>AlertRuleCtrl: POST /api/v1/alert-rules 🔒 ALERT_MANAGE
    Note right of AlertRuleSvc: ✏️ rx_alert_rule
    AlertRuleCtrl-->>U: 创建规则

    U->>AlertRuleCtrl: PUT /api/v1/alert-rules/{id}/toggle?enabled=true 🔒 ALERT_MANAGE
    Note right of AlertRuleSvc: ✏️ rx_alert_rule<br/>启停规则
    AlertRuleCtrl-->>U: 更新状态
```

---

## 4.4 巡检（Inspection） {#inspection}

```mermaid
sequenceDiagram
    actor U as 用户
    participant InspectionCtrl as InspectionController
    participant Svc as InspectionService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    U->>InspectionCtrl: POST /api/v1/inspection/execute 🔒 INSPECTION_EXECUTE
    InspectionCtrl->>Svc: 执行巡检任务
    Svc->>AS400Client: 按巡检模板执行检查项
    Note right of AS400Client: CL: DSPFD / DSPPGM / CHKOBJ<br/>QSYS2.SYSLIMITS
    AS400Client->>IBMi: 执行巡检
    IBMi-->>Svc: 巡检结果
    Note right of Svc: ✏️ rx_inspection_record
    InspectionCtrl-->>U: 巡检报告

    U->>InspectionCtrl: GET /api/v1/inspection/history 🔒 INSPECTION_VIEW
    Note right of Svc: 📖 rx_inspection_record
    InspectionCtrl-->>U: 巡检历史
```

### 涉及权限码

| 权限码 | 说明 |
|--------|------|
| `MONITOR_VIEW` | 监控查看 |
| `ALERT_MANAGE` | 告警规则管理 |
| `INSPECTION_VIEW` | 巡检查看 |
| `INSPECTION_EXECUTE` | 巡检执行 |

### 涉及数据表

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `rx_metric` | 📖 | 指标采集数据 |
| `rx_alert_rule` | 📖✏️ | 告警规则 |
| `rx_alert_event` | 📖 | 告警事件 |
| `rx_inspection_record` | 📖✏️ | 巡检记录 |
| `rx_ibmi_system` | 📖 | 服务器配置 |