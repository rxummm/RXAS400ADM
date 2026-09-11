package com.rxas400adm.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 服务器对比结果（MonitorController.compare 返回）。
 * 包含服务器元信息 + 当前指标快照。
 * 扁平结构：直接暴露常用指标字段，兼容前端 CompareSnapshot 类型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompareResultVO(
        Long id,
        String name,
        String host,
        String environment,
        String status,
        Long cpu,
        Long memory,
        Long disk,
        Long jobs,
        Long msgw,
        Long lckw,
        Map<String, Object> metrics
) {
}
