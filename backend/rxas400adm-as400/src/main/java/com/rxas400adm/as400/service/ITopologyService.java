package com.rxas400adm.as400.service;

import com.rxas400adm.as400.model.GraphData;

/**
 * 库级调用拓扑：基于 DSPPGMREF 聚合数据构建 PGM/SRVPGM/MODULE/FILE 调用关系图。
 * 数据源按 X-AS400-Server 路由。
 */
public interface ITopologyService {

    GraphData graph(String library);
}
