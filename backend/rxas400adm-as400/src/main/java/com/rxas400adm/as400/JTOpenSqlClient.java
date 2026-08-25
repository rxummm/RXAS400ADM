package com.rxas400adm.as400;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JTOpen SqlClient 委托实现（QSYS2 SQL 查询，含 H3/W4 异常分类）。
 */
@Slf4j
class JTOpenSqlClient implements SqlClient {

    private static final int QUERY_TIMEOUT_SECONDS = 30;

    private final JTOpenConnectionState state;

    JTOpenSqlClient(JTOpenConnectionState state) {
        this.state = state;
    }

    @Override
    public Map<String, Object> querySingle(String sql) {
        List<Map<String, Object>> rows = queryList(sql);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql) {
        try {
            return executeQuery(sql);
        } catch (SQLException | RuntimeException e) {
            log.error("IBM i SQL 查询失败(host={})，返回空数据: {}", state.host, state.redact(e.getMessage()));
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return queryList(sql);
        }
        try {
            return executeQuery(sql, params);
        } catch (SQLException | RuntimeException e) {
            log.error("IBM i 参数化 SQL 查询失败(host={})，返回空数据: {}", state.host, state.redact(e.getMessage()));
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        try {
            return executeQuery(sql, params);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            boolean connectionIssue = (sqlState != null && sqlState.startsWith("08"))
                    || e instanceof SQLNonTransientConnectionException
                    || e instanceof SQLTransientConnectionException
                    || e instanceof SQLRecoverableException;
            if (connectionIssue) {
                throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                        "IBM i 连接失败: " + state.redact(e.getMessage()));
            }
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行失败: " + state.redact(e.getMessage()));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    private List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        try (Connection conn = state.dataSource().getConnection()) {
            if (params == null || params.length == 0) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    try (ResultSet rs = ps.executeQuery()) {
                        return toRows(rs);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return toRows(rs);
                }
            }
        }
    }

    private List<Map<String, Object>> toRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(meta.getColumnLabel(i).toUpperCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}