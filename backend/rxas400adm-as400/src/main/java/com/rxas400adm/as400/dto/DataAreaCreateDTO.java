package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据区域创建 DTO。
 */
@Data
public class DataAreaCreateDTO {
    @Schema(description = "库名", example = "QSYS")
    private String library = "QSYS";
    @Schema(description = "数据区域名", example = "MYAREA")
    private String name;
    @Schema(description = "长度", example = "50")
    private int length = 50;
    @Schema(description = "初始值", example = "")
    private String value = "";
}