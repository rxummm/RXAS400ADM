package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图节点（拓扑分析 / 作业依赖图）。
 *
 * @param id      节点 ID（LIBRARY.NAME 全名）
 * @param name    节点名
 * @param type    节点类型（PGM/FILE/MSGF...）
 * @param library 所属库（可为空）
 */
public record GraphNode(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("library") String library) {
}
