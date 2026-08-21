package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * SLA 执行对比结果（JobSlaController.executions 返回）。
 */
public record SlaExecutionVO(Map<String, Object> data) {
    public static SlaExecutionVO from(Map<String, Object> map) {
        return new SlaExecutionVO(map);
    }
}
