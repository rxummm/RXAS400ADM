package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型写请求 DTO（create/update 共用）。
 * 不含 id/createdBy/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DictTypeDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "字典类型编码", example = "sys_status")
    private String code;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "字典类型名称", example = "系统状态")
    private String name;

    @Schema(description = "备注", example = "系统状态字典")
    private String remark;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;
}