package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SysvalRow;

import java.util.List;
import java.util.Map;

/**
 * 系统值域：QSYS2.SYSTEM_VALUE_INFO 只读查询 + CHGSYSVAL 修改（单条/批量）。
 */
public interface SysvalClient {

    /**
     * 系统值列表（QSYS2.SYSTEM_VALUE_INFO）。只读查询，失败返回空列表。
     */
    List<SysvalRow> listSystemValues();

    /** 修改系统值（CHGSYSVAL SYSVAL(x) VALUE('...')） */
    CommandResult changeSystemValue(String name, String value);

    /**
     * 批量修改系统值：逐条执行 CHGSYSVAL，返回每个系统值的修改结果。
     * @param updates 系统值名称→新值的映射
     * @return 系统值名称→修改结果的映射
n     */
    Map<String, CommandResult> batchChangeSystemValues(Map<String, String> updates);
}
