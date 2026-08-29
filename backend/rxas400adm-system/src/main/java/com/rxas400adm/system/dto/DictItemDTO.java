package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典项写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class DictItemDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "字典类型编码", example = "sys_status")
    private String typeCode;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "字典项键", example = "1")
    private String itemKey;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "字典项值", example = "正常")
    private String itemValue;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;
}