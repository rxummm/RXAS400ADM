package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型写请求 DTO（create/update 共用）。
 * 不含 id/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DictTypeDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String code;

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    private String remark;

    private Integer sort;

    private Integer status;
}
