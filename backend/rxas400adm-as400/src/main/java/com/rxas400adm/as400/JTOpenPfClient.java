package com.rxas400adm.as400;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;

import java.util.List;
import java.util.Map;

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

    private static String str(Map<String, Object> r, String key) {
        Object v = r.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static long lng(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v != null && !String.valueOf(v).isBlank()) {
            try {
                return Long.parseLong(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}