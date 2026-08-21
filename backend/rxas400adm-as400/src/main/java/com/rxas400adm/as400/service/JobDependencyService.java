package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.GraphData;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 作业依赖图（借鉴旧项目 jobDependencyGraph）：{ nodes, links } 图数据。
 * mock 模式返回仿真图；JT400 基于作业日志 CPF1124（提交关系）构建真实依赖图。
 * P1-7：CPF1124 全量作业日志扫描为重聚合查询，结果按服务器 60s 缓存（as400Aggregate）。
 */
@Service
@RequiredArgsConstructor
public class JobDependencyService {

    private static final String CACHE = "as400Aggregate";

    private final AS400ClientProvider clientProvider;

    @Cacheable(cacheNames = CACHE, keyGenerator = "serverAwareKeyGenerator")
    public GraphData graph() {
        return clientProvider.current().jobDependencies();
    }
}