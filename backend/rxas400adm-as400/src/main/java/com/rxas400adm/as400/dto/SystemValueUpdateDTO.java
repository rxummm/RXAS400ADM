package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统值修改请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class SystemValueUpdateDTO {
    @NotBlank(message = "system value is required")
    @Schema(description = "系统值", example = "2025-01-01")
    private String value;
}