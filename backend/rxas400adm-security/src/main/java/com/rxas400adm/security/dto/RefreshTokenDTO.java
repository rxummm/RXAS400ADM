package com.rxas400adm.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token 请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class RefreshTokenDTO {
    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "刷新令牌", example = "eyJhbGciOi...")
    private String refreshToken;
}