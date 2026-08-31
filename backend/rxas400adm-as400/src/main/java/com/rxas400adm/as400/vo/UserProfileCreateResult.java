package com.rxas400adm.as400.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * AS400用户Profile创建结果
 */
@Schema(description = "AS400用户Profile创建结果")
public record UserProfileCreateResult(
        @Schema(description = "是否成功") boolean success,
        @Schema(description = "消息") String message,
        @Schema(description = "用户名") String userName,
        @Schema(description = "创建时间") Instant createdTime) {

    public static UserProfileCreateResult success(String userName) {
        return new UserProfileCreateResult(true, "创建成功", userName, Instant.now());
    }

    public static UserProfileCreateResult fail(String userName, String message) {
        return new UserProfileCreateResult(false, message, userName, Instant.now());
    }
}
