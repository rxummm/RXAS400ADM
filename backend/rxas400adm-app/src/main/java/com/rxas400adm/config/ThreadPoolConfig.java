package com.rxas400adm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring 管理的线程池集中配置。
 * <p>
 * C-4 修复：将原来散落在 AlertWebhookListener / PlatformTaskController 的
 * Executors.newFixedThreadPool 替换为 Spring Bean，优势：
 * <ul>
 *   <li>Spring 容器管理生命周期（@PreDestroy 自动 shutdown）</li>
 *   <li>CallerRunsPolicy 替代 AbortPolicy（队列满时调用线程执行，不丢失任务也不抛异常）</li>
 *   <li>Bean 可被 @MockBean 注入方便测试</li>
 * </ul>
 * CollectorScheduler 因 poolSize 动态计算（来自配置），保留原地 DCL + CallerRunsPolicy 修复。
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 告警推送线程池（2 线程，队列容量 50）。
     * 用途：AlertWebhookListener 异步发送 Webhook / 邮件 / 站内通知。
     * C14：拒绝策略改 Discard+计数日志——CallerRuns 会让告警风暴时的分发
     * （Webhook 重试×sleep）跑回采集/Quartz 发布者线程，拖垮采集主链路；
     * 告警事实已先落 rx_alert_event（对账依据），丢推送优于拖垮采集。
     */
    @Bean("alertNotifyPool")
    public Executor alertNotifyPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("alert-notify-");
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler((r, exec) ->
                log.warn("[告警推送] 队列已满，丢弃一条推送任务"));
        executor.initialize();
        return executor;
    }

    /**
     * 【P3】健康/对比探测线程池（2~4 线程 daemon，health-probe-N）。
     * 用途：HealthService 多服务器并行连接测试 + MonitorService.compare 并行取 overview（共用并行度）。
     * 单台超时由 AS400 客户端自身超时控制，本池只做小扇出 fan-out；
     * CallerRunsPolicy：单次请求扇出小（≤20 台），打满时回调用线程执行即可，不丢任务。
     */
    @Bean("healthProbePool")
    public Executor healthProbePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("health-probe-");
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 平台任务手动触发线程池（4 线程，队列容量 20）。
     * 用途：PlatformTaskController 手动触发 @Scheduled 定时任务。
     */
    @Bean("platformTaskPool")
    public Executor platformTaskPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("platform-task-");
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
