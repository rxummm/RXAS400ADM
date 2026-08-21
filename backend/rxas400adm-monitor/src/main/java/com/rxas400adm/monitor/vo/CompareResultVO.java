package com.rxas400adm.monitor.vo;

import java.util.Map;

/**
 * 服务器对比结果（MonitorController.compare 返回）。
 * 包含服务器元信息 + 当前指标快照。
 */
public record CompareResultVO(
        Long id,
        String name,
        String host,
        String environment,
        String status,
        Map<String, Object> metrics
) {
}
