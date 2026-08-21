package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用户 profile 信息（组归属等）。用户不存在时返回 null。
 *
 * @param userName     用户名（USER_NAME）
 * @param groupProfile 组 profile（GROUP_PROFILE）
 * @param status       状态（STATUS，*ENABLED 等）
 */
public record UserProfileRow(
        @JsonProperty("USER_NAME") String userName,
        @JsonProperty("GROUP_PROFILE") String groupProfile,
        @JsonProperty("STATUS") String status) {
}
