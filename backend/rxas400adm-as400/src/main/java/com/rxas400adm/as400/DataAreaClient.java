package com.rxas400adm.as400;

import com.rxas400adm.as400.model.DataAreaRow;

import java.util.List;

/**
 * 数据区域域：QSYS2.DATA_AREA_INFO 查询 + CHGDTAARA/CRTDTAARA/DLTDTAARA 操作。
 */
public interface DataAreaClient {

    /**
     * 数据区域列表（QSYS2.DATA_AREA_INFO）。按库筛选，失败返回空列表。
     */
    List<DataAreaRow> listDataAreas(String library);

    /**
     * 数据区域详情（单个数据区域的值）。
     */
    DataAreaRow getDataArea(String library, String name);

    /**
     * 修改数据区域值（CHGDTAARA DTAARA(lib/name) VALUE('...')）。
     */
    CommandResult changeDataArea(String library, String name, String value);

    /**
     * 创建数据区域（CRTDTAARA DTAARA(lib/name) TYPE(*CHAR) LEN(n) VALUE('...')）。
     */
    CommandResult createDataArea(String library, String name, int length, String value);

    /**
     * 删除数据区域（DLTDTAARA DTAARA(lib/name)）。
     */
    CommandResult deleteDataArea(String library, String name);
}
