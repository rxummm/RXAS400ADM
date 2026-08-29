# 定时任务与后台采集

## 8.1 CollectorScheduler 后台采集流程

```mermaid
sequenceDiagram
    participant Scheduler as CollectorScheduler
    participant Lock as 分布式锁 (rx_dist_lock)
    participant SystemMapper as IbmiSystemMapper
    participant Collectors as MetricCollector[]
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant MetricSvc as MetricService
    participant AlertEngine as AlertEngine
    participant DB as 业务数据库

    Note over Scheduler: @Scheduled(fixedDelay=10s)
    Scheduler->>Scheduler: collect()

    Note over Scheduler: 分布式锁检查
    Scheduler->>Lock: tryLock(rx_monitor_collector, holder, TTL=60s)
    Note right of Lock: ✏️ rx_dist_lock<br/>原子 UPDATE 行影响数=1 表示 Leader
    alt 未获取锁（非 Leader）
        Scheduler->>Scheduler: return（跳过本轮）
    else 获取锁成功（Leader）
        Scheduler->>SystemMapper: selectList(enabled=true)
        Note right of SystemMapper: 📖 rx_ibmi_system
        SystemMapper-->>Scheduler: 启用的服务器列表

        alt 并行采集 (parallel=true && servers > 1)
            Scheduler->>Scheduler: collectParallel(systems)
            Note over Scheduler: CompletableFuture 并行<br/>每台服务器一个线程<br/>整轮超时 roundTimeoutMs
            loop 每台服务器 (并行)
                Scheduler->>Scheduler: collectServer(system)
                loop 每个 Collector
                    Scheduler->>Collectors: collector.collect(systemId)
                    Note over Collectors: CpuCollector / MemCollector / DiskCollector<br/>MsgwCollector / LckwCollector / ...
                    Collectors->>AS400Client: JTOpenSqlClient → DB2 for i
                    Note right of AS400Client: 🗄️ QSYS2.SYSTEM_STATUS_INFO<br/>QSYS2.ACTIVE_JOB_INFO<br/>等系统表
                    AS400Client->>IBMi: 查询指标
                    IBMi-->>Collectors: 指标数据
                    Collectors-->>Scheduler: Metric 对象
                    Scheduler->>MetricSvc: save(metric)
                    Note right of MetricSvc: ✏️ rx_metric
                    Scheduler->>AlertEngine: check(metric)
                    Note right of AlertEngine: 📖 rx_alert_rule<br/>规则匹配 + 阈值判断
                    alt 触发告警
                        AlertEngine->>AlertEngine: 创建告警事件
                        Note right of AlertEngine: ✏️ rx_alert_event<br/>⚡ WebSocket 推送<br/>⚡ Webhook 回调
                    end
                end
            end
        else 串行采集
            loop 每台服务器 (串行)
                Scheduler->>Scheduler: collectServer(system)
            end
        end

        Note over Scheduler: 释放分布式锁
        Scheduler->>Lock: releaseLock(rx_monitor_collector, holder)
        Note right of Lock: ✏️ rx_dist_lock<br/>校验 holder 归属后释放
    end
```

### 核心设计要点

| 设计点 | 说明 |
|--------|------|
| 分布式锁 | 基于数据库 `rx_dist_lock` 原子 UPDATE，多实例部署仅 Leader 采集 |
| 并行采集 | `CompletableFuture` 并行，每台服务器独立线程，整轮超时控制 |
| 指标采集器 | 策略模式，CPU/MEM/DISK/MSGW/LCKW 各自独立 Collector |
| 告警联动 | 采集后立即触发规则匹配，阈值超限自动创建告警事件 |
| 告警通知 | WebSocket 实时推送 + Webhook 回调（邮件/企业微信等） |

### 涉及数据表

| 数据表/系统表 | 操作 | 说明 |
|--------------|------|------|
| `rx_dist_lock` | 📖✏️ | 分布式锁（Leader 选举） |
| `rx_ibmi_system` | 📖 | 启用的服务器列表 |
| `QSYS2.SYSTEM_STATUS_INFO` | 🗄️ | CPU/内存/磁盘指标 |
| `QSYS2.ACTIVE_JOB_INFO` | 🗄️ | MSGW/LCKW 作业计数 |
| `rx_metric` | ✏️ | 写入采集指标 |
| `rx_alert_rule` | 📖 | 告警规则匹配 |
| `rx_alert_event` | ✏️ | 触发的告警事件 |