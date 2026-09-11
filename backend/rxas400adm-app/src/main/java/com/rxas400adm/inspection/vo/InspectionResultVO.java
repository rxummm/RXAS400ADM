package com.rxas400adm.inspection.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * 巡检结果（InspectionController.generate 返回）。
 * 服务层返回的动态巡检数据。
 */
public record InspectionResultVO(@JsonValue Map<String, Object> data) {
    public static InspectionResultVO from(Map<String, Object> map) {
        return new InspectionResultVO(map);
    }
}
