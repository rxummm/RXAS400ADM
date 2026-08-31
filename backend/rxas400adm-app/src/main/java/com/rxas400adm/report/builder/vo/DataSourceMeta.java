package com.rxas400adm.report.builder.vo;

import java.util.List;

/**
 * 数据源元数据 VO：告诉前端该数据源有哪些字段可选。
 */
public record DataSourceMeta(
        String key,
        String label,
        List<FieldMeta> fields
) {
    public record FieldMeta(String key, String label, String type) {
        /** type: dimension / measure / date */
    }
}
