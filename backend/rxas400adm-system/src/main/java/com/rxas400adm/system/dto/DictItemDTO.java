package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典项写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DictItemDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String typeCode;

    @NotBlank(message = "{validation.notBlank}")
    private String itemKey;

    @NotBlank(message = "{validation.notBlank}")
    private String itemValue;

    private Integer sort;

    private Integer status;
}
