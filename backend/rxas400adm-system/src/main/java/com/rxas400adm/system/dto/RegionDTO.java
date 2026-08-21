package com.rxas400adm.system.dto;

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
    private String code;

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    private Integer level;

    private String parentCode;

    private String pinyin;

    private String abbreviation;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer sort;

    private Integer status;
}
