package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文档模板写请求 DTO。
 * 不含 id/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DocTemplateDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "模板名称", example = "标准配置文档")
    private String name;

    @Schema(description = "模板分类", example = "系统配置")
    private String category;

    @Schema(description = "模板内容", example = "# 配置模板\n...")
    private String content;

    @Schema(description = "模板类型：MARKDOWN / TEXT / HTML", example = "MARKDOWN")
    private String docType;
}