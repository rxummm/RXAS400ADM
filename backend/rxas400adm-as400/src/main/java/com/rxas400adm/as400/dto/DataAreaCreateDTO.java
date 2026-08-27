package com.rxas400adm.as400.dto;

import lombok.Data;

/**
 * 数据区域创建 DTO。
 */
@Data
public class DataAreaCreateDTO {
    private String library = "QSYS";
    private String name;
    private int length = 50;
    private String value = "";
}
