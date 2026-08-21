package com.rxas400adm.inspection.vo;

import java.util.Map;

/**
 * 巡检报告（InspectionController.generate 返回）。
 * 服务层返回的结构化巡检数据，前端按 key 展示各维度结果。
 */
public record InspectionReportVO(Map<String, Object> data) {
    public static InspectionReportVO from(Map<String, Object> map) {
        return new InspectionReportVO(map);
    }
}
