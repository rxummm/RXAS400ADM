package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.SqlHistory;
import com.rxas400adm.as400.mapper.SqlHistoryMapper;
import com.rxas400adm.as400.vo.QueryResult;
import com.rxas400adm.common.constants.PageConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.rxas400adm.common.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QSYS2 SQL 执行器（追踪文档 2.4.1/2.4.2）：
 * 仅允许 SELECT 只读查询，通过 AS400Client.queryList 执行（mock 仿真 / JT400 真实 JDBC），
 * 结果记录到 rx_sql_history（按当前登录用户）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlQueryService implements ISqlQueryService {

    private static final int MAX_ROWS = 200;

    private final AS400ClientProvider clientProvider;
    private final SqlHistoryMapper historyMapper;

    public QueryResult execute(String sql) {
        SqlReadOnlyValidator.assertReadOnly(sql);
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = client.queryListChecked(sql.trim());
        long costMs = System.currentTimeMillis() - start;

        // 列名去重保序（前端表格列）
        Set<String> columnSet = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columnSet.addAll(row.keySet());
        }
        List<String> columns = new ArrayList<>(columnSet);
        List<Map<String, Object>> limited = rows.size() > MAX_ROWS ? rows.subList(0, MAX_ROWS) : rows;

        saveHistory(sql == null ? "" : sql.trim(), rows.size(), costMs);
        return QueryResult.builder()
                .columns(columns)
                .rows(limited)
                .rowsReturned(rows.size())
                .costMs(costMs)
                .build();
    }

    /** P2-12：SQL 历史按用户隔离——只返回当前登录用户自己的查询记录 */
    public List<SqlHistory> history(int limit) {
        return historyMapper.selectList(new LambdaQueryWrapper<SqlHistory>()
                .eq(SqlHistory::getOperator, SecurityUtils.currentUsername())
                .orderByDesc(SqlHistory::getCreatedTime)
                .last(PageConstants.limitClause(Math.max(1, Math.min(limit, 200)))));
    }

    private void saveHistory(String sql, int rows, long costMs) {
        SqlHistory history = new SqlHistory();
        history.setSqlText(sql.length() > 4000 ? sql.substring(0, 4000) : sql);
        history.setRowsReturned(rows);
        history.setCostMs(costMs);
        history.setOperator(SecurityUtils.currentUsername());
        history.setCreatedTime(LocalDateTime.now());
        try {
            historyMapper.insert(history);
        } catch (Exception e) {
            log.warn("SQL 历史记录失败: {}", e.getMessage());
        }
    }


}
