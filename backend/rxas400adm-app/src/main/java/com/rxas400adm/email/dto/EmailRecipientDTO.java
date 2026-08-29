package com.rxas400adm.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加分组成员 DTO。
 */
@Data
public class EmailRecipientDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Email(message = "{validation.email}")
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;

    @Schema(description = "用户ID", example = "1")
    private Long userId;
}