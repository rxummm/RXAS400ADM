package com.rxas400adm.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C-AS400-023: MSGW 消息抓取线程池 Spring Bean 管理。
 * 替代 JobService 中的 static ExecutorService，纳入容器生命周期（@PreDestroy 优雅关闭）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(As400ThreadPoolProperties.class)
public class As400ThreadPoolConfig {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final As400ThreadPoolProperties properties;
    private ExecutorService executor;

    @Bean("msgwExecutor")
    public ExecutorService msgwExecutor() {
        this.executor = new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveSeconds(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getQueueCapacity()),
                r -> {
                    Thread t = new Thread(r, "msgw-fetch-" + SEQ.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t != null) {
                    log.warn("[msgwExecutor] task failed: {}", t.getMessage());
                }
            }
        };
        return this.executor;
    }

    @PreDestroy
    public void shutdown() {
        log.info("[msgwExecutor] shutting down...");
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
