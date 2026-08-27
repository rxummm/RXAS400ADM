package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 数据区域行（QSYS2.DATA_AREA_INFO）。
 *
 * @param library   库名（DATA_AREA_LIBRARY）
 * @param name      数据区域名（DATA_AREA_NAME）
 * @param type      类型（DATA_AREA_TYPE，如 *CHAR/*INT/*DEC）
 * @param length    长度（DATA_AREA_LENGTH）
 * @param value     当前值（DATA_AREA_VALUE）
 * @param description 描述（DATA_AREA_DESCRIPTION）
 */
public record DataAreaRow(
        @JsonProperty("DATA_AREA_LIBRARY") String library,
        @JsonProperty("DATA_AREA_NAME") String name,
        @JsonProperty("DATA_AREA_TYPE") String type,
        @JsonProperty("DATA_AREA_LENGTH") int length,
        @JsonProperty("DATA_AREA_VALUE") String value,
        @JsonProperty("DATA_AREA_DESCRIPTION") String description) {
}
