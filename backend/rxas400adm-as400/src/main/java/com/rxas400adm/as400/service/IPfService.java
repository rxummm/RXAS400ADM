package com.rxas400adm.as400.service;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import com.rxas400adm.as400.model.PfStatsRow;
import com.rxas400adm.common.response.PageResult;

import java.util.List;
import java.util.Map;

/**
 * PF 物理文件浏览：文件列表 / 字段 / 记录查看 / 统计信息。
 * 数据源按 X-AS400-Server 路由。
 */
public interface IPfService {

    List<PfRow> files(String library);

    List<PfColumnRow> columns(String library, String file);

    List<Map<String, Object>> data(String library, String file, int limit);

    PageResult<Map<String, Object>> dataPage(String library, String file, int current, int size);

    PfStatsRow statistics(String library, String file);
}
