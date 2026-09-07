package com.rxas400adm.as400.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * QSYS2 SQL 执行结果：列名 + 行数据 + 耗时/行数（用于前端结果表格）。
 */
@Data
@Builder
public class QueryResult {

    private List<String> columns;

    private List<Map<String, Object>> rows;

    private Integer rowsReturned;

    private Long costMs;
}
