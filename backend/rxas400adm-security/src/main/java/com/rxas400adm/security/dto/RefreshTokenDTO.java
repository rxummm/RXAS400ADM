package com.rxas400adm.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token 请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class RefreshTokenDTO {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
