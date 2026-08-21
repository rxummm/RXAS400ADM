package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统公告写请求 DTO（create/update 共用）。
 * 不含 id/publishedTime/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class NoticeDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String title;

    private String content;

    /** 1=发布（即时推送）/0=草稿 */
    private Integer status;
}
