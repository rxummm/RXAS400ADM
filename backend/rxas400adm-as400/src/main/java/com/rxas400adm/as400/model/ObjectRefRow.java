package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 对象引用分析行（DSPPGMREF）。
 *
 * @param library    源对象库（OBJECT_LIBRARY）
 * @param name       源对象名（OBJECT_NAME）
 * @param type       源对象类型（OBJECT_TYPE）
 * @param refLibrary 被引用/引用者库（REF_OBJ_LIBRARY）
 * @param refName    被引用/引用者对象名（REF_OBJ_NAME）
 * @param refType    被引用/引用者对象类型（REF_OBJ_TYPE）
 */
public record ObjectRefRow(
        @JsonProperty("OBJECT_LIBRARY") String library,
        @JsonProperty("OBJECT_NAME") String name,
        @JsonProperty("OBJECT_TYPE") String type,
        @JsonProperty("REF_OBJ_LIBRARY") String refLibrary,
        @JsonProperty("REF_OBJ_NAME") String refName,
        @JsonProperty("REF_OBJ_TYPE") String refType) {
}
