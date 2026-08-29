package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 行政区划写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class RegionDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "区划编码", example = "110000")
    private String code;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "区划名称", example = "北京市")
    private String name;

    @Schema(description = "层级", example = "1")
    private Integer level;

    @Schema(description = "父级编码", example = "0")
    private String parentCode;

    @Schema(description = "拼音", example = "beijing")
    private String pinyin;

    @Schema(description = "简称", example = "京")
    private String abbreviation;

    @Schema(description = "经度", example = "116.4")
    private BigDecimal longitude;

    @Schema(description = "纬度", example = "39.9")
    private BigDecimal latitude;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;
}