package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PF 物理文件行（2.4.5）。
 *
 * @param tableName 表名（TABLE_NAME）
 * @param schema    库名（TABLE_SCHEMA）
 * @param text      表描述（TABLE_TEXT）
 */
public record PfRow(
        @JsonProperty("TABLE_NAME") String tableName,
        @JsonProperty("TABLE_SCHEMA") String schema,
        @JsonProperty("TABLE_TEXT") String text) {
}
