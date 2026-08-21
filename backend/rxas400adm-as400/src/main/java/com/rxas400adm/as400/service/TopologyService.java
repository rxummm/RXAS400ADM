package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.GraphData;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 库级调用拓扑（3.7，参照旧项目 As400TopologyController）：
 * 基于 DSPPGMREF 聚合数据构建 PGM/SRVPGM/MODULE/FILE 调用关系图，
 * 用于改动影响面分析与排障定位。数据源按 X-AS400-Server 路由。M2：返回 GraphData。
 * P1-7：聚合查询结果按服务器 60s 缓存（as400Aggregate）。
 */
@Service
@RequiredArgsConstructor
public class TopologyService implements ITopologyService {

    private static final String CACHE = "as400Aggregate";

    private final AS400ClientProvider clientProvider;

    @Cacheable(cacheNames = CACHE, keyGenerator = "serverAwareKeyGenerator")
    public GraphData graph(String library) {
        return clientProvider.current().objectGraph(library);
    }
}
