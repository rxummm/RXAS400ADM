package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 对象详情（DSPOBJD 属性）。对象不存在时返回 null（调用方兜底空对象）。
 *
 * @param name              对象名（OBJECT_NAME）
 * @param type              对象类型（OBJECT_TYPE）
 * @param library           所属库（OBJECT_LIBRARY）
 * @param size              对象大小字节（OBJECT_SIZE）
 * @param creationTimestamp 创建时间（OBJECT_CREATION_TIMESTAMP）
 * @param changeTimestamp   最后修改时间（OBJECT_CHANGE_TIMESTAMP）
 * @param description       文本描述（OBJECT_TEXT_DESCRIPTION）
 * @param owner             属主（OBJECT_OWNER）
 * @param asp               ASP（ASP_NAME）
 */
public record ObjectDetail(
        @JsonProperty("OBJECT_NAME") String name,
        @JsonProperty("OBJECT_TYPE") String type,
        @JsonProperty("OBJECT_LIBRARY") String library,
        @JsonProperty("OBJECT_SIZE") long size,
        @JsonProperty("OBJECT_CREATION_TIMESTAMP") String creationTimestamp,
        @JsonProperty("OBJECT_CHANGE_TIMESTAMP") String changeTimestamp,
        @JsonProperty("OBJECT_TEXT_DESCRIPTION") String description,
        @JsonProperty("OBJECT_OWNER") String owner,
        @JsonProperty("ASP_NAME") String asp) {
}
