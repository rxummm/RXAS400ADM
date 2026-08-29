package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档写请求 DTO。
 * 仅业务字段可写；id/version/status/rejectReason/deleted/deletedTime/
 * createdBy/updatedBy/approvedBy/approvedTime/createdTime/updatedTime 全部服务端托管。
 */
@Data
public class DocDTO {

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "模板ID", example = "1")
    private Long templateId;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "文档标题", example = "服务器配置文档")
    private String title;

    @Schema(description = "文档内容", example = "# 配置说明\n...")
    private String content;

    @Schema(description = "文档类型：MARKDOWN / TEXT / HTML / PDF / IMAGE", example = "MARKDOWN")
    private String docType;

    @Schema(description = "上传到IFS的路径", example = "/home/doc/config.md")
    private String ifsPath;
}