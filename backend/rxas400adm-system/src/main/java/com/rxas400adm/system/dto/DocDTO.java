package com.rxas400adm.system.dto;

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
    private Long templateId;

    @NotBlank(message = "{validation.notBlank}")
    private String title;

    private String content;

    /** 文档类型：MARKDOWN / TEXT / HTML / PDF / IMAGE（缺省按 MARKDOWN 处理） */
    private String docType;

    /** 上传到 IFS 的路径（上传成功后 update 携带） */
    private String ifsPath;
}
