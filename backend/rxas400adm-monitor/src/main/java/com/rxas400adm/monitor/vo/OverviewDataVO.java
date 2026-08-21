package com.rxas400adm.monitor.vo;

import java.util.Map;

/**
 * 监控概览数据（MonitorController.overview 返回）。
 * 服务层返回的动态指标聚合。
 */
public record OverviewDataVO(Map<String, Object> data) {
    public static OverviewDataVO from(Map<String, Object> map) {
        return new OverviewDataVO(map);
    }
}
