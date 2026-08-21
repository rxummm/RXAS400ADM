package com.rxas400adm.monitor.vo;

import java.util.Map;

/**
 * 监控概览（MonitorController.overview 返回）。
 * 服务层返回的动态结构化监控数据。
 */
public record MonitorOverviewVO(Map<String, Object> data) {
    public static MonitorOverviewVO from(Map<String, Object> map) {
        return new MonitorOverviewVO(map);
    }
}
