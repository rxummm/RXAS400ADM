package com.rxas400adm.monitor.vo;

import java.util.Map;

/**
 * 容量趋势数据（MonitorController.capacity 返回）。
 */
public record CapacityTrendVO(Map<String, Object> data) {
    public static CapacityTrendVO from(Map<String, Object> map) {
        return new CapacityTrendVO(map);
    }
}
