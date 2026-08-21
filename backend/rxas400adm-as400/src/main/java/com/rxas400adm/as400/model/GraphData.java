package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 图数据（拓扑分析 objectGraph / 作业依赖 jobDependencies）。
 *
 * @param nodes 节点列表
 * @param links 边列表
 */
public record GraphData(
        @JsonProperty("nodes") List<GraphNode> nodes,
        @JsonProperty("links") List<GraphLink> links) {

    /** 空图（不可达/失败时返回，前端渲染空态） */
    public static GraphData empty() {
        return new GraphData(List.of(), List.of());
    }
}
