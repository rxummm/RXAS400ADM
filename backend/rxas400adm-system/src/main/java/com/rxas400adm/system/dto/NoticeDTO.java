package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统公告写请求 DTO（create/update 共用）。
 * 不含 id/publishedTime/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class NoticeDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "公告标题", example = "系统维护通知")
    private String title;

    @Schema(description = "公告内容", example = "系统将于今晚进行维护")
    private String content;

    @Schema(description = "1=发布（即时推送）/0=草稿", example = "1")
    private Integer status;
}