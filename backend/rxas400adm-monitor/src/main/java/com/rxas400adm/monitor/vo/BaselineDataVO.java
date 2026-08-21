package com.rxas400adm.monitor.vo;

import java.util.Map;

/**
 * 性能基线数据（MonitorController.baseline 返回）。
 */
public record BaselineDataVO(Map<String, Object> data) {
    public static BaselineDataVO from(Map<String, Object> map) {
        return new BaselineDataVO(map);
    }
}
