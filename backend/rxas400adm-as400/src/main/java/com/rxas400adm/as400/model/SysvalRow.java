package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 系统值行（QSYS2.SYSTEM_VALUE_INFO）。
 *
 * @param name        系统值名称（SYSTEM_VALUE_NAME）
 * @param value       当前值（CURRENT_VALUE）
 * @param description 值描述（VALUE_DESCRIPTION）
 * @param type        值类型（SYSTEM_VALUE_TYPE）
 */
public record SysvalRow(
        @JsonProperty("SYSTEM_VALUE_NAME") String name,
        @JsonProperty("CURRENT_VALUE") String value,
        @JsonProperty("VALUE_DESCRIPTION") String description,
        @JsonProperty("SYSTEM_VALUE_TYPE") String type) {
}
