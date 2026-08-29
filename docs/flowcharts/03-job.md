# 作业中心

## 3.1 活动作业列表

`GET /api/v1/jobs`

```mermaid
sequenceDiagram
    actor U as 用户
    participant JobView as job/index.vue
    participant JobCtrl as JobController
    participant JobSvc as JobService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>JobView: 进入作业中心
    JobView->>JobCtrl: GET /api/v1/jobs?status=ACTIVE<br/>🔒 JOB_VIEW
    JobCtrl->>JobSvc: activeJobs(status)
    JobSvc->>AS400Client: JTOpenCommandClient
    Note right of AS400Client: CL: WRKACTJOB → OUTPUT(*PRINT)<br/>或 QSYS2.ACTIVE_JOB_INFO
    AS400Client->>IBMi: 查询活动作业
    IBMi-->>AS400Client: 作业列表
    AS400Client-->>JobSvc: List<JobInfo>
    JobSvc-->>JobCtrl: 作业列表
    JobCtrl-->>JobView: 活动作业数据

    Note over JobView: 支持 MSGW/LCKW 过滤
    JobView->>JobCtrl: GET /api/v1/jobs/msgw
    JobCtrl->>JobSvc: msgwJobs()
    JobSvc->>AS400Client: WRKACTJOB MSGW(*YES)
    JobCtrl-->>JobView: MSGW 作业列表

    JobView->>JobCtrl: GET /api/v1/jobs/lckw
    JobCtrl->>JobSvc: lckwJobs()
    JobSvc->>AS400Client: WRKACTJOB LCKW(*YES)
    JobCtrl-->>JobView: LCKW 作业列表
```

---

## 3.2 作业控制与日志

ENDJOB / HLDJOB / RLSJOB / DSPJOBLOG / MSGW 应答

```mermaid
sequenceDiagram
    actor U as 用户
    participant JobView as job/index.vue
    participant JobCtrl as JobController
    participant JobSvc as JobService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    Note over U: 作业详情
    U->>JobView: 点击作业行
    JobView->>JobCtrl: GET /api/v1/jobs/{name}/{user}/{number}<br/>🔒 JOB_VIEW
    JobCtrl->>JobSvc: jobDetail(name, user, number)
    JobSvc->>AS400Client: QSYS2.ACTIVE_JOB_INFO
    AS400Client->>IBMi: 查询作业详情
    IBMi-->>JobView: 作业详情

    Note over U: 结束作业
    U->>JobView: 点击 ENDJOB
    JobView->>JobCtrl: POST /api/v1/jobs/{name}/{user}/{number}/end<br/>🔒 JOB_END
    JobCtrl->>JobSvc: endJob(name, user, number)
    JobSvc->>AS400Client: JTOpenCommandClient.execute("ENDJOB ...")
    Note right of AS400Client: CL: ENDJOB JOB(user/number/name)<br/>标识符校验: requireIdentifier()
    AS400Client->>IBMi: 结束作业
    IBMi-->>JobView: CommandResult

    Note over U: 挂起/释放作业
    JobView->>JobCtrl: POST /api/v1/jobs/{...}/hold (HLDJOB) 🔒 JOB_END
    JobView->>JobCtrl: POST /api/v1/jobs/{...}/release (RLSJOB) 🔒 JOB_END

    Note over U: 作业日志
    U->>JobView: 查看作业日志
    JobView->>JobCtrl: GET /api/v1/jobs/{name}/{user}/{number}/log<br/>🔒 JOB_VIEW
    JobCtrl->>JobSvc: jobLog(name, user, number)
    JobSvc->>AS400Client: DSPJOBLOG OUTPUT(*PRINT)
    AS400Client->>IBMi: 获取作业日志
    IBMi-->>JobView: 日志列表

    Note over U: 作业队列
    JobView->>JobCtrl: GET /api/v1/jobs/queues 🔒 JOB_VIEW
    JobCtrl->>JobSvc: jobQueues()
    JobSvc->>AS400Client: QSYS2.JOB_QUEUE_INFO
    JobCtrl-->>JobView: 作业队列列表
```

---

## 3.3 SPOOL 文件与管理

```mermaid
sequenceDiagram
    actor U as 用户
    participant JobView as job/index.vue
    participant JobCtrl as JobController
    participant JobSvc as JobService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>JobView: 查看 SPOOL 文件
    JobView->>JobCtrl: GET /api/v1/jobs/{name}/{user}/{number}/spool
    JobCtrl->>JobSvc: spoolFiles(name, user, number)
    JobSvc->>AS400Client: QSYS2.OUTPUT_QUEUE_ENTRIES_BASIC
    AS400Client->>IBMi: 查询 SPOOL
    IBMi-->>JobView: SPOOL 列表

    U->>JobView: 查看 SPOOL 内容
    JobView->>JobCtrl: GET /api/v1/jobs/{name}/{user}/{number}/spool/{id}/content
    JobSvc->>AS400Client: CPYSPLF → IFS → 读取内容
    JobCtrl-->>JobView: SPOOL 文本内容

    U->>JobView: 应答 MSGW 消息
    JobView->>JobCtrl: POST /api/v1/jobs/msgw/{name}/{user}/{number}/reply
    JobSvc->>AS400Client: RPLMSG (CL命令)
    AS400Client->>IBMi: 应答消息
```

---

## 3.4 Job SLA 与依赖 {#job-sla}

```mermaid
sequenceDiagram
    actor U as 用户
    participant JobCtrl as JobSlaController
    participant JobDepCtrl as JobDependencyController
    participant Svc as Service
    participant DB as 业务数据库

    Note over U: Job SLA 管理
    U->>JobCtrl: GET /api/v1/job-sla 🔒 JOB_VIEW
    JobCtrl->>Svc: 查询 SLA 规则
    Note right of Svc: 📖 rx_job_sla
    JobCtrl-->>U: SLA 规则列表

    U->>JobCtrl: POST /api/v1/job-sla 🔒 JOB_MANAGE
    Note right of Svc: ✏️ rx_job_sla
    JobCtrl-->>U: 创建 SLA 规则

    Note over U: 作业依赖管理
    U->>JobDepCtrl: GET /api/v1/job-dependencies 🔒 JOB_VIEW
    JobDepCtrl->>Svc: 查询作业依赖关系
    Note right of Svc: 📖 rx_job_dependency
    JobDepCtrl-->>U: 依赖关系列表
```

### 涉及权限码

| 权限码 | 说明 |
|--------|------|
| `JOB_VIEW` | 作业查看 |
| `JOB_END` | 作业控制（结束/挂起/释放） |
| `JOB_MANAGE` | SLA 管理 |

### 涉及数据表

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `QSYS2.ACTIVE_JOB_INFO` | 🗄️ | 活动作业列表 |
| `QSYS2.JOB_QUEUE_INFO` | 🗄️ | 作业队列信息 |
| `QSYS2.OUTPUT_QUEUE_ENTRIES_BASIC` | 🗄️ | SPOOL 文件列表 |
| `rx_job_sla` | 📖✏️ | SLA 规则 |
| `rx_job_dependency` | 📖 | 作业依赖关系 |