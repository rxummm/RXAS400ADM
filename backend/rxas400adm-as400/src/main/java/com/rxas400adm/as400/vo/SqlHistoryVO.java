package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.SqlHistory;

import java.time.LocalDateTime;

/**
 * SQL 查询历史视图（P2-10）：与 SqlHistory 字段一致。
 */
public record SqlHistoryVO(
        Long id,
        String sqlText,
        Integer rowsReturned,
        Long costMs,
        String operator,
        LocalDateTime createdTime) {

    public static SqlHistoryVO from(SqlHistory e) {
        return new SqlHistoryVO(
                e.getId(), e.getSqlText(), e.getRowsReturned(), e.getCostMs(),
                e.getOperator(), e.getCreatedTime());
    }
}
