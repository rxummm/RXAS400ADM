package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * 作业日志条目（JobController.log 返回）。
 * 服务层返回 SQL 查询的动态结果。
 */
public record JobLogEntryVO(Map<String, Object> data) {
    public static JobLogEntryVO from(Map<String, Object> map) {
        return new JobLogEntryVO(map);
    }
}
