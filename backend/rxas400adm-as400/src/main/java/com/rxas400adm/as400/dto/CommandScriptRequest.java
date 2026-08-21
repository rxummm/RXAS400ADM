package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 命令脚本创建/更新请求（2.4.3）。
 */
@Data
public class CommandScriptRequest {

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    private String description;

    @NotBlank(message = "{validation.notBlank}")
    private String command;

    private Boolean favorite;

    /** 逗号分隔标签 */
    private String tags;
}
