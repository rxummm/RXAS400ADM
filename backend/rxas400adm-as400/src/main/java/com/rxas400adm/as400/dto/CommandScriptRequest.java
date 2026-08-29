package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 命令脚本创建/更新请求（2.4.3）。
 */
@Data
public class CommandScriptRequest {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "脚本名称", example = "备份脚本")
    private String name;

    @Schema(description = "脚本描述", example = "每日备份数据")
    private String description;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "CL命令", example = "SBMJOB CMD(CALL PGM(BACKUP))")
    private String command;

    @Schema(description = "是否收藏")
    private Boolean favorite;

    @Schema(description = "逗号分隔标签", example = "备份,日常")
    private String tags;
}