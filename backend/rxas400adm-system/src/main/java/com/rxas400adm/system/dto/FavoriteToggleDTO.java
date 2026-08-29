package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 收藏切换请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class FavoriteToggleDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Schema(description = "收藏标题", example = "用户管理")
    private String title;

    @Schema(description = "图标", example = "user")
    private String icon;
}