package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PF 字段行（2.4.5）。
 *
 * @param name     字段名（COLUMN_NAME）
 * @param type     数据类型（COLUMN_TYPE）
 * @param length   长度（LENGTH）
 * @param nullable 可空（NULLABLE，Y/N）
 */
public record PfColumnRow(
        @JsonProperty("COLUMN_NAME") String name,
        @JsonProperty("COLUMN_TYPE") String type,
        @JsonProperty("LENGTH") int length,
        @JsonProperty("NULLABLE") String nullable) {
}
