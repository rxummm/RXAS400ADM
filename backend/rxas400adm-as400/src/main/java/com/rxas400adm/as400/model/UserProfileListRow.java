package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用户 profile 列表行（QSYS2.USER_INFO）。
 *
 * @param userName    用户名
 * @param status      状态（*ENABLED/*DISABLED）
 * @param groupProfile 组 profile
 * @param description 描述
 * @param lastUsedDate 最后使用日期
 */
public record UserProfileListRow(
        @JsonProperty("USER_NAME") String userName,
        @JsonProperty("STATUS") String status,
        @JsonProperty("GROUP_PROFILE") String groupProfile,
        @JsonProperty("TEXT_DESCRIPTION") String description,
        @JsonProperty("LAST_USED_DATE") String lastUsedDate) {
}
