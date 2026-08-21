package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 收藏切换请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class FavoriteToggleDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String path;

    private String title;

    private String icon;
}
