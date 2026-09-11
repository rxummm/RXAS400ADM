package com.rxas400adm.as400.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * 作业日志条目（JobController.log 返回）。
 */
public record JobLogVO(@JsonValue Map<String, Object> data) {
    public static JobLogVO from(Map<String, Object> map) {
        return new JobLogVO(map);
    }
}
