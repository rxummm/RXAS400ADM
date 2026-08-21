package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图边（拓扑分析 / 作业依赖图）。
 *
 * @param source 起点（LIBRARY.NAME 全名）
 * @param target 终点（LIBRARY.NAME 全名）
 */
public record GraphLink(
        @JsonProperty("source") String source,
        @JsonProperty("target") String target) {
}
