package com.rxas400adm.as400.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * P1-7：Spring Cache 缓存配置。
 * 低频聚合查询（作业 SLA 执行、作业依赖图、对象调用拓扑）结果按服务器缓存 60 秒，
 * 避免多用户重复触发 DB2 全量日志/对象统计重聚合。
 * 仅用进程内 Caffeine（单实例部署足够），不引入 Redis；
 * 管理端点见 app 模块 CacheController（SYS_CACHE_MANAGE）。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(1000));
        return manager;
    }
}