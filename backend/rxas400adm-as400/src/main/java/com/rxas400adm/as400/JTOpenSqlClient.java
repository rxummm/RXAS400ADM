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

    /**
     * E1：不再「吞错返空集」——故障伪装成合法空数据会让采集器落 0 值指标、告警引擎基于假数据判断。
     * 失败统一抛 BusinessException（连接类错误单独码），调用方（采集器/巡检）按需降级，
     * 监控侧表现为该 tick 数据点缺失（gap），而非致命的假 0 值。
     */
    @Override
    public List<Map<String, Object>> queryList(String sql) {
        try {
            return executeQuery(sql, null);
        } catch (SQLException e) {
            throw toSqlBusinessException(e);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return queryList(sql);
        }
        try {
            return executeQuery(sql, null, params);
        } catch (SQLException e) {
            throw toSqlBusinessException(e);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        try {
            return executeQuery(sql, null, params);
        } catch (SQLException e) {
            throw toSqlBusinessException(e);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    /** S7：服务端行数上限下推（JDBC setMaxRows）——大表查询不再全量拉回 JVM 内存后才截断 */
    @Override
    public List<Map<String, Object>> queryListCheckedBounded(String sql, int maxRows, Object... params) {
        try {
            return executeQuery(sql, maxRows <= 0 ? null : maxRows, params);
        } catch (SQLException e) {
            throw toSqlBusinessException(e);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public Map<String, Object> querySingleChecked(String sql, Object... params) {
        List<Map<String, Object>> rows = queryList(sql, params);
        if (rows.isEmpty()) {
            throw new IllegalStateException("查询结果为空: " + sql);
        }
        return rows.get(0);
    }

    @Override
    public Long queryForObject(String sql, Class<Long> type, Object... params) {
        List<Map<String, Object>> rows = queryList(sql, params);
        if (rows.isEmpty()) return 0L;
        Object val = rows.get(0).values().iterator().next();
        return val != null ? ((Number) val).longValue() : 0L;
    }

    @Override
    public int executeUpdate(String sql, Object... params) {
        try (Connection conn = state.pooledConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                }
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw toSqlBusinessException(e);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AS400_SQL_FAILED,
                    "IBM i SQL 执行异常: " + state.redact(e.getMessage()));
        }
    }

    /** SQLException → BusinessException：连接类错误（SQLState 08xxx）单独映射，消息统一脱敏 */
    private BusinessException toSqlBusinessException(SQLException e) {
        String sqlState = e.getSQLState();
        String msg = String.valueOf(e.getMessage());
        boolean connectionIssue = (sqlState != null && sqlState.startsWith("08"))
                || e instanceof SQLNonTransientConnectionException
                || e instanceof SQLTransientConnectionException
                || e instanceof SQLRecoverableException
                // 终检 1b：invalidate/disconnect 关池后借出连接报 "has been closed"（SQLState=null），
                // 归连接类错误而非普通 SQL 失败，避免前端误判为语法/权限问题
                || (sqlState == null && msg.contains("closed"));
        if (connectionIssue) {
            return new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                    "IBM i 连接失败: " + state.redact(e.getMessage()));
        }
        return new BusinessException(ErrorCode.AS400_SQL_FAILED,
                "IBM i SQL 执行失败: " + state.redact(e.getMessage()));
    }

    /** maxRows 非空时 JDBC 层截断（S7），避免大结果集整体进堆 */
    private List<Map<String, Object>> executeQuery(String sql, Integer maxRows, Object... params) throws SQLException {
        // P1：改用 Hikari 连接池取连接（原 dataSource().getConnection() 每次物理新建 TCP+signon）
        try (Connection conn = state.pooledConnection()) {
            if (params == null || params.length == 0) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    if (maxRows != null) {
                        ps.setMaxRows(maxRows);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        return toRows(rs);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                if (maxRows != null) {
                    ps.setMaxRows(maxRows);
                }
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