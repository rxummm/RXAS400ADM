package com.rxas400adm.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * 容量趋势数据（MonitorController.capacity 返回）。
 * 扁平结构：直接暴露字段，兼容前端 CapacityResponse 类型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CapacityTrendVO(
        List<Map<String, Object>> points,
        List<Map<String, Object>> prediction,
        Integer daysToThreshold
) {
    @SuppressWarnings("unchecked")
    public static CapacityTrendVO from(Map<String, Object> map) {
        return new CapacityTrendVO(
                (List<Map<String, Object>>) map.get("points"),
                (List<Map<String, Object>>) map.get("prediction"),
                map.get("daysToThreshold") != null ? Integer.parseInt(map.get("daysToThreshold").toString()) : null
        );
    }
}
