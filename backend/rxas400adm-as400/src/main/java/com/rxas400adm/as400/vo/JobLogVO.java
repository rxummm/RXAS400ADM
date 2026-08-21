package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * 作业日志条目（JobController.log 返回）。
 */
public record JobLogVO(Map<String, Object> data) {
    public static JobLogVO from(Map<String, Object> map) {
        return new JobLogVO(map);
    }
}
