package com.rxas400adm.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
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
public class As400ThreadPoolConfig {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Bean("msgwExecutor")
    public ExecutorService msgwExecutor() {
        return new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
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
    }

    @PreDestroy
    public void shutdown() {
        log.info("[msgwExecutor] shutting down...");
    }
}
