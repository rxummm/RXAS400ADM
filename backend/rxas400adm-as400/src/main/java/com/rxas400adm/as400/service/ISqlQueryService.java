package com.rxas400adm.as400.service;

import com.rxas400adm.as400.entity.SqlHistory;
import com.rxas400adm.as400.vo.QueryResult;

import java.util.List;

/**
 * QSYS2 SQL 执行器：仅允许 SELECT 只读查询，结果记录到 rx_sql_history（按当前登录用户）。
 */
public interface ISqlQueryService {

    QueryResult execute(String sql);

    List<SqlHistory> history(int limit);
}
