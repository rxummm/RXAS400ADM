package com.rxas400adm.as400;

import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境 SourceClient 实现（QSYS2 SQL 视图）。
 * <p>
 * IBM i 源码管理层次：Library → Source File（*SRCPF）→ Member（*SRC）。
 * 通过 QSYS2 系统目录视图查询，无需直接调用 CL 命令。
 */
@Slf4j
class JTOpenSourceClient implements SourceClient {

    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private final JTOpenConnectionState state;

    JTOpenSourceClient(JTOpenConnectionState state) {
        this.state = state;
    }

    /**
     * 列出库：查询 QSYS2.SYSTABLES 中有源文件的库名（去重）。
     * 为避免全量扫描，仅查询包含 *SRCPF 类型表的库。
     */
    @Override
    public List<String> listLibraries() {
        return queryStrings(SqlStatementRegistry.of("source.library.list"));
    }

    /**
     * 列出源文件：查询指定库中的 *SRCPF（FILE_TYPE = 'S'）。
     */
    @Override
    public List<String> listSourceFiles(String library) {
        String lib = JTOpenConnectionState.requireIdentifier(library, "库名");
        return queryStrings(SqlStatementRegistry.of("source.file.list"), lib);
    }

    /**
     * 列出源成员：查询指定源文件中的成员。
     */
    @Override
    public List<String> listMembers(String library, String sourceFile) {
        String lib = JTOpenConnectionState.requireIdentifier(library, "库名");
        String file = JTOpenConnectionState.requireIdentifier(sourceFile, "源文件名");
        return queryStrings(SqlStatementRegistry.of("source.member.list"), lib, file);
    }

    /**
     * 读取源成员内容：通过 QSYS2.SYSMEMBER 获取 MEMBER_DEFINITION（CLOB）。
     */
    @Override
    public String readMember(String library, String sourceFile, String member) {
        String lib = JTOpenConnectionState.requireIdentifier(library, "库名");
        String file = JTOpenConnectionState.requireIdentifier(sourceFile, "源文件名");
        String mbr = JTOpenConnectionState.requireIdentifier(member, "成员名");
        String sql = SqlStatementRegistry.of("source.member.content");
        try (Connection conn = state.pooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setString(1, lib);
            ps.setString(2, file);
            ps.setString(3, mbr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString(1);
                    return content != null ? content : "";
                }
                return "";
            }
        } catch (SQLException e) {
            log.error("IBM i 读取源成员失败(host={}, {}/{}/{}): {}",
                    state.host, lib, file, mbr, state.redact(e.getMessage()));
            throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                    "读取源成员失败: " + lib + "/" + file + "/" + mbr + " - " + e.getMessage());
        }
    }

    // ---- 工具方法 ----

    private List<String> queryStrings(String sql) {
        try (Connection conn = state.pooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    String val = rs.getString(1);
                    if (val != null && !val.isBlank()) {
                        result.add(val.trim());
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            log.error("IBM i SQL 查询失败(host={}): {}", state.host, state.redact(e.getMessage()));
            throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                    "IBM i SQL query failed: " + state.redact(e.getMessage()));
        }
    }

    private List<String> queryStrings(String sql, String... params) {
        try (Connection conn = state.pooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    String val = rs.getString(1);
                    if (val != null && !val.isBlank()) {
                        result.add(val.trim());
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            log.error("IBM i SQL 参数化查询失败(host={}): {}", state.host, state.redact(e.getMessage()));
            throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                    "IBM i SQL parameterized query failed: " + state.redact(e.getMessage()));
        }
    }
}
