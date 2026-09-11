package com.rxas400adm.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 监控概览数据（MonitorController.overview 返回）。
 * 服务层返回的动态指标聚合。
 * 扁平结构：直接暴露常用字段，兼容前端 MetricOverview 类型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OverviewDataVO(
        Long cpu,
        Long memory,
        Long disk,
        Long msgw,
        Long lckw,
        Long jobs,
        Map<String, Object> extra
) {
    public static OverviewDataVO from(Map<String, Object> map) {
        return new OverviewDataVO(
                toLong(map.get("cpu")),
                toLong(map.get("memory")),
                toLong(map.get("disk")),
                toLong(map.get("msgw")),
                toLong(map.get("lckw")),
                toLong(map.get("jobs")),
                map
        );
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
