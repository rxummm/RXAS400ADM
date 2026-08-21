package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * 列信息（BusinessController.columns 返回）。
 * SQL 查询 QSYS2.SYSCOLUMNS 的动态结果。
 */
public record ColumnDetailVO(Map<String, Object> data) {
    public static ColumnDetailVO from(Map<String, Object> map) {
        return new ColumnDetailVO(map);
    }
}
