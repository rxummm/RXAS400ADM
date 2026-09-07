package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 翻译条目写请求 DTO（新增/覆盖共用）。主键由 (lang, i18nKey) 复合决定。
 */
@Data
public class I18nEntryDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "i18n key", example = "menu.system")
    private String i18nKey;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Language", example = "zh-CN")
    private String lang;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Translation text", example = "System Management")
    private String text;

    @Schema(description = "Module", example = "menu")
    private String module;
}
