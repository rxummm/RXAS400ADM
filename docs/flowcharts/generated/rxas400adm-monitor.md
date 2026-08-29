# rxas400adm-monitor 模块（自动生成）

> ⚠️ 此文件由 `scripts/generate-flow-diagrams.cjs` 自动生成，请勿手动编辑。

> 生成时间：2026-08-28T15:50:32.339Z

## root

### PUT /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AlertRuleController

    Note over F: PUT /{id}
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: PUT /{id}
```

### DELETE /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AlertRuleController

    Note over F: DELETE /{id}
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: DELETE /{id}
```

### GET /capacity

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /capacity
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /capacity
```

### GET /compare

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /compare
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /compare
```

### GET /alerts

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /alerts
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /alerts
```

## toggle

### PUT /{id}/toggle

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AlertRuleController

    Note over F: PUT /{id}/toggle
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: PUT /{id}/toggle
```

## v1

### ALL /api/v1/alert-rules

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AlertRuleController

    Note over F: ALL /api/v1/alert-rules
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: ALL /api/v1/alert-rules
```

### ALL /api/v1/monitor

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: ALL /api/v1/monitor
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: ALL /api/v1/monitor
```

## {id}

### GET /overview/{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /overview/{id}
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /overview/{id}
```

### GET /metrics/{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /metrics/{id}
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /metrics/{id}
```

### GET /baseline/{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as MonitorController

    Note over F: GET /baseline/{id}
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /baseline/{id}
```
