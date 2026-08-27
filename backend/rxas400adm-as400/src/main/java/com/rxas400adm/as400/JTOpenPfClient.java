package com.rxas400adm.as400;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import com.rxas400adm.as400.model.PfStatsRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen PfClient 委托实现（物理文件列表 / 字段定义 / 记录分页查看）。
 */
class JTOpenPfClient implements PfClient {

    private final JTOpenSqlClient sqlClient;

    JTOpenPfClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
    }

    @Override
    public List<PfRow> listPfFiles(String library) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        return sqlClient.queryList("SELECT TABLE_NAME, TABLE_SCHEMA, TABLE_TEXT "
                + "FROM QSYS2.SYSTABLES WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_TYPE = 'P' FETCH FIRST 200 ROWS ONLY", lib).stream()
                .map(r -> new PfRow(str(r, "TABLE_NAME"), str(r, "TABLE_SCHEMA"), str(r, "TABLE_TEXT")))
                .toList();
    }

    @Override
    public List<PfColumnRow> pfColumns(String library, String file) {
        if (file == null || file.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        return sqlClient.queryList("SELECT COLUMN_NAME, COLUMN_TYPE, LENGTH, NULLABLE "
                + "FROM QSYS2.SYSCOLUMNS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION", lib, file.trim().toUpperCase()).stream()
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
        int capped = Math.max(1, Math.min(limit, 200));
        return sqlClient.queryList("SELECT * FROM " + lib + "." + JTOpenConnectionState.requireIdentifier(file, "文件/表名")
                + " FETCH FIRST " + capped + " ROWS ONLY");
    }

    @Override
    public PfStatsRow pfStatistics(String library, String file) {
        if (file == null || file.isBlank()) {
            return new PfStatsRow(0, 0, 0, List.of(), 0);
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        String tbl = file.trim().toUpperCase();

        // 记录数
        long recordCount = 0;
        try {
            var rows = sqlClient.queryList(
                    "SELECT COUNT(*) AS CNT FROM " + lib + "." + tbl);
            if (!rows.isEmpty() && rows.get(0).get("CNT") instanceof Number n) {
                recordCount = n.longValue();
            }
        } catch (Exception ignored) {
        }

        // 存储大小（从 QSYS2.SYSTABLES 获取行数估算，精确值需 DSPFD）
        long storageSize = 0;
        try {
            var rows = sqlClient.queryList(
                    "SELECT DATA_SPACE_SIZE, NUMBER_MEMBERS FROM QSYS2.SYSTABLES"
                            + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", lib, tbl);
            if (!rows.isEmpty()) {
                if (rows.get(0).get("DATA_SPACE_SIZE") instanceof Number n) {
                    storageSize = n.longValue() * 1024; // KB → bytes
                }
            }
        } catch (Exception ignored) {
        }

        // 索引信息
        List<String> indexNames = new ArrayList<>();
        int indexCount = 0;
        try {
            var rows = sqlClient.queryList(
                    "SELECT INDEX_NAME FROM QSYS2.SYSINDEXES"
                            + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", lib, tbl);
            indexCount = rows.size();
            for (var row : rows) {
                String name = str(row, "INDEX_NAME");
                if (!name.isEmpty()) {
                    indexNames.add(name);
                }
            }
        } catch (Exception ignored) {
        }

        // 成员数
        int memberCount = 1;
        try {
            var rows = sqlClient.queryList(
                    "SELECT NUMBER_MEMBERS FROM QSYS2.SYSTABLES"
                            + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", lib, tbl);
            if (!rows.isEmpty() && rows.get(0).get("NUMBER_MEMBERS") instanceof Number n) {
                memberCount = n.intValue();
            }
        } catch (Exception ignored) {
        }

        return new PfStatsRow(recordCount, storageSize, indexCount, indexNames, memberCount);
    }
}