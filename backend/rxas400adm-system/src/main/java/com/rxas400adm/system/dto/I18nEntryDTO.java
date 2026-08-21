package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 翻译条目写请求 DTO（新增/覆盖共用）。主键由 (lang, i18nKey) 复合决定。
 */
@Data
public class I18nEntryDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String i18nKey;

    @NotBlank(message = "{validation.notBlank}")
    private String lang;

    @NotBlank(message = "{validation.notBlank}")
    private String text;
}
