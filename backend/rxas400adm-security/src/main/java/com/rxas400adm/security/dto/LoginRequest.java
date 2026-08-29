package com.rxas400adm.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "密码", example = "pass1234")
    private String password;
}