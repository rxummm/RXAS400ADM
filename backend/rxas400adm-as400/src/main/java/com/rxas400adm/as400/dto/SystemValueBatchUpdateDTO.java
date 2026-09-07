package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 系统值批量修改请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class SystemValueBatchUpdateDTO {

    @NotEmpty(message = "batch update list is required")
    @Schema(description = "批量修改条目列表")
    private List<@Valid SystemValueEntryDTO> entries;

    @Data
    public static class SystemValueEntryDTO {
        @NotBlank(message = "system value name is required")
        @Schema(description = "系统值名称", example = "QDATE")
        private String name;

        @NotBlank(message = "system value is required")
        @Schema(description = "系统值", example = "2025-01-01")
        private String value;
    }
}