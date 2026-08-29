# rxas400adm-app 模块（自动生成）

> ⚠️ 此文件由 `scripts/generate-flow-diagrams.cjs` 自动生成，请勿手动编辑。

> 生成时间：2026-08-28T15:50:32.312Z

## root

### DELETE /{name}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as CacheController

    Note over F: DELETE /{name}
    Note over P1: 🔒 SYS_CACHE_MANAGE
    F->>P1: DELETE /{name}
```

### POST /test-send

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailConfigController

    Note over F: POST /test-send
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: POST /test-send
```

### GET /all

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: GET /all
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: GET /all
```

### PUT /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: PUT /{id}
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: PUT /{id}
```

### DELETE /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: DELETE /{id}
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: DELETE /{id}
```

### GET /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailLogController

    Note over F: GET /{id}
    Note over P1: 🔒 EMAIL_VIEW
    F->>P1: GET /{id}
```

### GET /generate

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as InspectionController

    Note over F: GET /generate
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /generate
```

### GET /export

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as InspectionController

    Note over F: GET /export
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: GET /export
```

### GET /metrics

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportController

    Note over F: GET /metrics
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: GET /metrics
```

### GET /executions

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportController

    Note over F: GET /executions
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: GET /executions
```

### GET /capacity

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportController

    Note over F: GET /capacity
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: GET /capacity
```

### PUT /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: PUT /{id}
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: PUT /{id}
```

### DELETE /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: DELETE /{id}
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: DELETE /{id}
```

## v1

### ALL /api/v1/caches

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as CacheController

    Note over F: ALL /api/v1/caches
    Note over P1: 🔒 SYS_CACHE_MANAGE
    F->>P1: ALL /api/v1/caches
```

### ALL /api/v1/health

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as HealthController

    Note over F: ALL /api/v1/health
    Note over P1: 🔒 HEALTH_VIEW
    F->>P1: ALL /api/v1/health
```

### ALL /api/v1/tasks

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as PlatformTaskController

    Note over F: ALL /api/v1/tasks
    Note over P1: 🔒 SYS_TASK_MANAGE
    F->>P1: ALL /api/v1/tasks
```

### ALL /api/v1/email/config

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailConfigController

    Note over F: ALL /api/v1/email/config
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: ALL /api/v1/email/config
```

### ALL /api/v1/email/groups

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: ALL /api/v1/email/groups
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: ALL /api/v1/email/groups
```

### ALL /api/v1/email/logs

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailLogController

    Note over F: ALL /api/v1/email/logs
    Note over P1: 🔒 EMAIL_VIEW
    F->>P1: ALL /api/v1/email/logs
```

### ALL /api/v1/email/send

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailSendController

    Note over F: ALL /api/v1/email/send
    Note over P1: 🔒 EMAIL_SEND
    F->>P1: ALL /api/v1/email/send
```

### ALL /api/v1/inspection

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as InspectionController

    Note over F: ALL /api/v1/inspection
    Note over P1: 🔒 MONITOR_VIEW
    F->>P1: ALL /api/v1/inspection
```

### ALL /api/v1/reports

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportController

    Note over F: ALL /api/v1/reports
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: ALL /api/v1/reports
```

### ALL /api/v1/report-schedules

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: ALL /api/v1/report-schedules
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: ALL /api/v1/report-schedules
```

## {methodName}

### POST /{beanName}/{methodName}/trigger

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as PlatformTaskController

    Note over F: POST /{beanName}/{methodName}/trigger
    Note over P1: 🔒 SYS_TASK_MANAGE
    F->>P1: POST /{beanName}/{methodName}/trigger
```

## members

### GET /{id}/members

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: GET /{id}/members
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: GET /{id}/members
```

### POST /{id}/members

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: POST /{id}/members
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: POST /{id}/members
```

### DELETE /{id}/members/{memberId}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as EmailGroupController

    Note over F: DELETE /{id}/members/{memberId}
    Note over P1: 🔒 EMAIL_MANAGE
    F->>P1: DELETE /{id}/members/{memberId}
```

## toggle

### PUT /{id}/toggle

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: PUT /{id}/toggle
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: PUT /{id}/toggle
```

## execute

### POST /{id}/execute

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: POST /{id}/execute
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: POST /{id}/execute
```

## history

### GET /{id}/history

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as ReportScheduleController

    Note over F: GET /{id}/history
    Note over P1: 🔒 REPORT_VIEW
    F->>P1: GET /{id}/history
```
