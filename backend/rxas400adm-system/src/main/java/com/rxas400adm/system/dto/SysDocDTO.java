package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库文档写请求 DTO。
 */
@Data
public class SysDocDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "文档标题", example = "系统使用手册")
    private String title;

    @Schema(description = "文档内容", example = "# 使用手册\n...")
    private String content;

    @Schema(description = "文档分类", example = "系统管理")
    private String category;

    @Schema(description = "标签", example = "手册,入门")
    private String tags;

    @Schema(description = "状态：DRAFT / PUBLISHED", example = "PUBLISHED")
    private String status;
}