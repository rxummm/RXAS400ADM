package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SysvalRow;

import java.util.List;

/**
 * 系统值域：QSYS2.SYSTEM_VALUE_INFO 只读查询 + CHGSYSVAL 修改。
 */
public interface SysvalClient {

    /**
     * 系统值列表（QSYS2.SYSTEM_VALUE_INFO）。只读查询，失败返回空列表。
     */
    List<SysvalRow> listSystemValues();

    /** 修改系统值（CHGSYSVAL SYSVAL(x) VALUE('...')） */
    CommandResult changeSystemValue(String name, String value);
}
