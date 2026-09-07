package com.rxas400adm.as400;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import com.rxas400adm.as400.model.PfStatsRow;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.common.constants.PageConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen PfClient 委托实现（物理文件列表 / 字段定义 / 记录分页查看）。
 */
@Slf4j
class JTOpenPfClient implements PfClient {

    private final JTOpenSqlClient sqlClient;

    JTOpenPfClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
    }

    @Override
    public List<PfRow> listPfFiles(String library) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        return sqlClient.queryList(SqlStatementRegistry.of("pf.table.list"), lib).stream()
                .map(r -> new PfRow(str(r, "TABLE_NAME"), str(r, "TABLE_SCHEMA"), str(r, "TABLE_TEXT")))
                .toList();
    }

    @Override
    public List<PfColumnRow> pfColumns(String library, String file) {
        if (file == null || file.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : JTOpenConnectionState.requireIdentifier(library, "库名");
        String tbl = JTOpenConnectionState.requireIdentifier(file, "文件/表名");
        return sqlClient.queryList(SqlStatementRegistry.of("pf.column.list"), lib, tbl).stream()
                .map(r -> new PfColumnRow(str(r, "COLUMN_NAME"), str(r, "COLUMN_TYPE"),
                        (int) lng(r, "LENGTH"), str(r, "NULLABLE")))
                .toList();
    }

    @Override
    public List<Map<String, Object>> pfData(String library, String file, int limit) {
        if (file == null || file.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : JTOpenConnectionState.requireIdentifier(library, "库名");
        int capped = Math.max(1, Math.min(limit, PageConstants.MAX_PF_DATA_LIMIT));
        return sqlClient.queryList(SqlStatementRegistry.of("pf.data.read").replace("{lib}", lib).replace("{tbl}", JTOpenConnectionState.requireIdentifier(file, "文件/表名"))
                + " FETCH FIRST " + capped + " ROWS ONLY");
    }

    @Override
    public PfStatsRow pfStatistics(String library, String file) {
        if (file == null || file.isBlank()) {
            return new PfStatsRow(0, 0, 0, List.of(), 0);
        }
        String lib = library == null || library.isBlank() ? "QSYS" : JTOpenConnectionState.requireIdentifier(library, "库名");
        String tbl = JTOpenConnectionState.requireIdentifier(file, "文件/表名");

        // 记录数
        long recordCount = 0;
        try {
            var rows = sqlClient.queryList(
                    SqlStatementRegistry.of("pf.count").replace("{lib}", lib).replace("{tbl}", tbl));
            if (!rows.isEmpty() && rows.get(0).get("CNT") instanceof Number n) {
                recordCount = n.longValue();
            }
        } catch (Exception e) {
            log.debug("PF record count query failed for {}.{}: {}", lib, tbl, e.getMessage());
        }

        // 存储大小（从 QSYS2.SYSTABLES 获取行数估算，精确值需 DSPFD）
        long storageSize = 0;
        try {
            var rows = sqlClient.queryList(
                    SqlStatementRegistry.of("pf.storage.info"), lib, tbl);
            if (!rows.isEmpty()) {
                if (rows.get(0).get("DATA_SPACE_SIZE") instanceof Number n) {
                    storageSize = n.longValue() * 1024; // KB → bytes
                }
            }
        } catch (Exception e) {
            log.debug("PF storage info query failed for {}.{}: {}", lib, tbl, e.getMessage());
        }

        // 索引信息
        List<String> indexNames = new ArrayList<>();
        int indexCount = 0;
        try {
            var rows = sqlClient.queryList(
                    SqlStatementRegistry.of("pf.index.list"), lib, tbl);
            indexCount = rows.size();
            for (var row : rows) {
                String name = str(row, "INDEX_NAME");
                if (!name.isEmpty()) {
                    indexNames.add(name);
                }
            }
        } catch (Exception e) {
            log.debug("PF index list query failed for {}.{}: {}", lib, tbl, e.getMessage());
        }

        // 成员数
        int memberCount = 1;
        try {
            var rows = sqlClient.queryList(
                    SqlStatementRegistry.of("pf.member.count")
                            + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", lib, tbl);
            if (!rows.isEmpty() && rows.get(0).get("NUMBER_MEMBERS") instanceof Number n) {
                memberCount = n.intValue();
            }
        } catch (Exception e) {
            log.debug("PF member count query failed for {}.{}: {}", lib, tbl, e.getMessage());
        }

        return new PfStatsRow(recordCount, storageSize, indexCount, indexNames, memberCount);
    }
}