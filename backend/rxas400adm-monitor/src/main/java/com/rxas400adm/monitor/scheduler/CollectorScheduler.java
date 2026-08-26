package com.rxas400adm.monitor.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.monitor.alert.AlertEngine;
import com.rxas400adm.monitor.collector.MetricCollector;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.service.IMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PreDestroy;

/**
 * 统一调度采集：遍历所有启用的 IBM i 服务器执行各 Collector。
 * <p>
 * P2：per-server 并行采集（{@code rxas400.monitor.collect-parallel=true} 时启用），
 * 每台服务器一个线程并行执行全部 Collector，整轮带超时（{@code rxas400.monitor.collect-round-timeout-ms}），
 * 避免真机模式下多服务器串行拖慢整轮、单台不可达挂死调度线程。
 * <p>
 * 生产环境配合表级分布式锁 rx_dist_lock（rxas400.monitor.scheduler-lock=true 时启用，
 * 兼容 MySQL 与 DB2 for i，替代原 MySQL GET_LOCK），多节点下同一时刻仅 Leader 采集，
 * 避免指标重复写入 + 告警重复触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorScheduler {

    private static final String COLLECT_LOCK = "rx_monitor_collector";

    /** H2：表级锁 TTL（秒）——持有超过该时长未释放视为失效，其他节点可抢占 */
    private static final long LOCK_TTL_SECONDS = 60;

    private final List<MetricCollector> collectors;
    private final IMetricService metricService;
    private final IbmiSystemMapper systemMapper;
    private final AlertEngine alertEngine;
    private final MetricMapper metricMapper;

    /** P2：是否并行采集（默认开，单服务器时自动退化为串行） */
    @Value("${rxas400.monitor.collect-parallel:true}")
    private boolean parallel = true;

    /** P2：并行线程池大小（上限与服务器数对齐） */
    @Value("${rxas400.monitor.collect-pool-size:8}")
    private int poolSize = 8;

    /** P2：整轮采集超时（毫秒），防止单台不可达服务器拖死整轮 */
    @Value("${rxas400.monitor.collect-round-timeout-ms:30000}")
    private long roundTimeoutMs = 30000;

    private volatile ExecutorService collectPool;

    /** C2：采集中的服务器 ID 集合——上一轮未完成则本轮跳过，防止超时残留任务与新任务重叠 */
    private final Set<Long> inFlightServers = ConcurrentHashMap.newKeySet();

    @PreDestroy
    private void shutdown() {
        ExecutorService p = collectPool;
        if (p != null && !p.isShutdown()) {
            p.shutdownNow();
            try {
                p.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private ExecutorService pool() {
        ExecutorService p = collectPool;
        if (p == null) {
            synchronized (this) {
                p = collectPool;
                if (p == null) {
                    AtomicInteger seq = new AtomicInteger();
                    ThreadFactory factory = r -> {
                        Thread t = new Thread(r, "metric-collector-" + seq.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    };
                    p = new ThreadPoolExecutor(
                            Math.max(2, poolSize), Math.max(2, poolSize),
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<>(100),
                            factory,
                            new ThreadPoolExecutor.CallerRunsPolicy());
                    collectPool = p;
                }
            }
        }
        return p;
    }

    @Scheduled(fixedDelayString = "${rxas400.monitor.collect-interval-ms:10000}")
    public void collect() {
        // 分布式锁：仅 Leader 执行采集（false 跳过本轮，避免多节点重复写入/告警）
        // P2-18：环境变量优先（部署平台注入优先级高于 JVM 属性），未配置时默认关闭
        String envLock = System.getenv("RXAS400_SCHEDULER_LOCK");
        boolean lockEnabled = "true".equalsIgnoreCase(
                envLock != null ? envLock : System.getProperty("rxas400.monitor.scheduler-lock", "false"));
        // H2：每轮生成独立持有者标识，release 校验归属，防误释放他人锁
        String holder = lockEnabled ? nodeId() : null;
        if (lockEnabled && !tryLock(holder)) {
            return;
        }
        try {
            List<IbmiSystem> systems = systemMapper.selectList(
                    new LambdaQueryWrapper<IbmiSystem>().eq(IbmiSystem::getEnabled, true));
            if (systems.isEmpty()) {
                return;
            }
            if (parallel && systems.size() > 1) {
                collectParallel(systems);
            } else {
                for (IbmiSystem system : systems) {
                    collectServer(system);
                }
            }
        } finally {
            if (lockEnabled) {
                releaseLock(holder);
            }
        }
    }

    /** P2：per-server 并行采集，整轮超时（C2：超时取消在途任务 + in-flight 跨轮去重） */
    private void collectParallel(List<IbmiSystem> systems) {
        ExecutorService executor = pool();
        List<CompletableFuture<Void>> futures = systems.stream()
                .map(system -> CompletableFuture.runAsync(() -> runServerGuarded(system), executor))
                .toList();
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.get(roundTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // C2：超时后取消在途任务——半死服务器的阻塞调用不能跨轮残留占线程池，
            // 否则下一轮重叠提交同一服务器，新旧两份 collectServer 并发写指标/评估告警
            futures.forEach(f -> f.cancel(true));
            log.warn("采集轮次超时（{}ms），已取消未完成服务器", roundTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // collectServer 内部已吞掉单台异常，理论上不会到这里；兜底记录
            // 【E5-5】追加异常对象保留堆栈（ExecutionException 的 getMessage 仅是 cause 的 toString）
            log.warn("采集并行任务异常: {}", e.getMessage(), e);
        }
    }

    /** C2：同一服务器上一轮未完成则本轮跳过（in-flight 标记），杜绝新旧两份采集并发 */
    private void runServerGuarded(IbmiSystem system) {
        if (!inFlightServers.add(system.getId())) {
            log.warn("服务器 {} 上一轮采集未完成，本轮跳过", system.getName());
            return;
        }
        try {
            collectServer(system);
        } finally {
            inFlightServers.remove(system.getId());
        }
    }

    private void collectServer(IbmiSystem system) {
        // P5：两段式——先全量收集再一次批量入库，替代每采集器逐条 INSERT
        List<Metric> metrics = new ArrayList<>();
        for (MetricCollector collector : collectors) {
            try {
                Metric metric = collector.collect(system.getId());
                if (metric == null) {
                    continue;
                }
                // 终检：跳过非有限数值（NaN/Infinity）——多值 INSERT 中一条脏数据会拖垮整批，
                // 导致该服务器本轮 0 入库/0 告警评估
                if (metric.getMetricValue() != null && !Double.isFinite(metric.getMetricValue())) {
                    log.warn("丢弃非有限指标值 {}[{}]={}", system.getName(),
                            metric.getMetricName(), metric.getMetricValue());
                    continue;
                }
                metrics.add(metric);
            } catch (Exception e) {
                // C15：恢复中断位，保证 shutdownNow / cancel(true) 时任务能感知取消
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("采集失败 {}[{}]: {}", system.getName(), collector.name(), e.getMessage());
            }
        }
        metricService.saveBatch(metrics);
        // P5：入库后按收集顺序逐条评估告警（告警顺序语义与原实现一致）
        for (Metric metric : metrics) {
            try {
                alertEngine.check(metric);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("告警评估失败 {}[{}]: {}", system.getName(), metric.getMetricName(), e.getMessage());
            }
        }
    }

    /** H2：抢表级锁——原子 UPDATE 行影响数 = 1 表示本节点获得 Leader 资格。 */
    private boolean tryLock(String holder) {
        try {
            int updated = metricMapper.tryAcquireLock(COLLECT_LOCK, holder,
                    LocalDateTime.now().plusSeconds(LOCK_TTL_SECONDS));
            boolean acquired = updated == 1;
            if (acquired) {
                log.info("[采集锁] 本节点获得 Leader 资格 {}", holder);
            }
            return acquired;
        } catch (Exception e) {
            log.warn("[采集锁] 抢锁检查失败，按单节点模式继续: {}", e.getMessage());
            return true;
        }
    }

    private void releaseLock(String holder) {
        try {
            metricMapper.releaseLock(COLLECT_LOCK, holder);
        } catch (Exception e) {
            log.warn("[采集锁] 释放锁失败: {}", e.getMessage());
        }
    }

    private String nodeId() {
        String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return pid + "-" + UUID.randomUUID().toString().substring(0, 4);
    }
}