package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * IFS 文件写入 / 创建目录请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class IfsWriteDTO {
    @NotBlank(message = "IFS 路径不能为空")
    @Size(max = 500, message = "路径长度不能超过 500 个字符")
    @Schema(description = "IFS路径", example = "/home/user/file.txt")
    private String path;

    @Schema(description = "文件内容（创建目录时可为空）", example = "Hello World")
    private String content;
}