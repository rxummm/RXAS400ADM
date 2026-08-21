package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 对象权限行（DSPOBJAUT）。
 *
 * @param holder     权限持有者（AUTHORITY_HOLDER）
 * @param holderType 持有者类型 *USER/*GROUP/*PUBLIC（AUTHORITY_HOLDER_TYPE）
 * @param authority  权限（AUTHORITY）
 */
public record AuthorityRow(
        @JsonProperty("AUTHORITY_HOLDER") String holder,
        @JsonProperty("AUTHORITY_HOLDER_TYPE") String holderType,
        @JsonProperty("AUTHORITY") String authority) {
}
