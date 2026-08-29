package com.rxas400adm.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "旧密码", example = "oldPass123")
    private String oldPassword;

    @NotBlank(message = "{validation.notBlank}")
    @Size(min = 8, message = "{validation.password.length}")
    @Schema(description = "新密码", example = "newPass123")
    private String newPassword;
}