package com.rxas400adm.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOi...")
    private String token;
    @Schema(description = "刷新令牌（仅在登录/刷新时返回）", example = "eyJhbGciOi...")
    private String refreshToken;
    @Schema(description = "Access token过期时间（毫秒）", example = "3600000")
    private Long expireMs;
    @Schema(description = "用户名", example = "admin")
    private String username;
    @Schema(description = "权限列表", example = "[\"USER_EDIT\", \"ROLE_MANAGE\"]")
    private List<String> permissions;
}