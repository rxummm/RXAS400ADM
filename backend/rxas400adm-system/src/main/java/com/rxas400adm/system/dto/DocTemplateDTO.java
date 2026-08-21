package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文档模板写请求 DTO。
 * 不含 id/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DocTemplateDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    private String category;

    private String content;

    /** 模板类型：MARKDOWN / TEXT / HTML（缺省按 MARKDOWN 处理） */
    private String docType;
}
