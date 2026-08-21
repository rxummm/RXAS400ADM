package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 对象搜索结果行（DSPOBJD / QSYS2.OBJECT_STATISTICS）。
 *
 * @param name              对象名（OBJECT_NAME）
 * @param type              对象类型（OBJECT_TYPE）
 * @param library           所属库（OBJECT_LIBRARY）
 * @param size              对象大小字节（OBJECT_SIZE）
 * @param creationTimestamp 创建时间（OBJECT_CREATION_TIMESTAMP）
 */
public record ObjectRow(
        @JsonProperty("OBJECT_NAME") String name,
        @JsonProperty("OBJECT_TYPE") String type,
        @JsonProperty("OBJECT_LIBRARY") String library,
        @JsonProperty("OBJECT_SIZE") long size,
        @JsonProperty("OBJECT_CREATION_TIMESTAMP") String creationTimestamp) {
}
