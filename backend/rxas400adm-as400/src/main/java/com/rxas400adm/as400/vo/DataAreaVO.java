package com.rxas400adm.as400.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据区域 VO（替代 Entity 返回前端）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataAreaVO {
    private String library;
    private String name;
    private String value;
    private String type;
    private int length;
}
