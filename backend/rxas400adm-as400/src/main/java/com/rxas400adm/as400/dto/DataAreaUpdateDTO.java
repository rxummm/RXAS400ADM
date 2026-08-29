package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据区域修改 DTO。
 */
@Data
public class DataAreaUpdateDTO {
    @Schema(description = "数据区域值", example = "newValue")
    private String value;
}