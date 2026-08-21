package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * 执行记录条目（ExecutionController.list 返回）。
 * 合并调度执行与脚本执行的统一视图。
 */
public record ExecutionRecordVO(Map<String, Object> data) {
    public static ExecutionRecordVO from(Map<String, Object> map) {
        return new ExecutionRecordVO(map);
    }
}
