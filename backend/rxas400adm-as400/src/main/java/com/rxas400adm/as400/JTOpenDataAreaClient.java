package com.rxas400adm.as400;

import com.rxas400adm.as400.model.DataAreaRow;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.JTOpenConnectionState.str;

/**
 * JTOpen DataAreaClient 委托实现（数据区域查询与修改）。
 */
@Slf4j
class JTOpenDataAreaClient implements DataAreaClient {

    private final JTOpenSqlClient sqlClient;
    private final JTOpenCommandClient commandClient;
    JTOpenDataAreaClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
        this.commandClient = new JTOpenCommandClient(state);
    }

    @Override
    public List<DataAreaRow> listDataAreas(String library) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        List<Map<String, Object>> rows = sqlClient.queryList(
                SqlStatementRegistry.of("dataarea.list"), lib);
        List<DataAreaRow> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new DataAreaRow(
                    str(row, "DATA_AREA_LIBRARY"),
                    str(row, "DATA_AREA_NAME"),
                    str(row, "DATA_AREA_TYPE"),
                    (int) JTOpenConnectionState.lng(row, "DATA_AREA_LENGTH"),
                    str(row, "DATA_AREA_VALUE"),
                    str(row, "DATA_AREA_DESCRIPTION")));
        }
        return result;
    }

    @Override
    public DataAreaRow getDataArea(String library, String name) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        String n = name == null ? "" : name.trim().toUpperCase();
        List<Map<String, Object>> rows = sqlClient.queryList(
                SqlStatementRegistry.of("dataarea.detail"), lib, n);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        return new DataAreaRow(
                str(row, "DATA_AREA_LIBRARY"),
                str(row, "DATA_AREA_NAME"),
                str(row, "DATA_AREA_TYPE"),
                (int) JTOpenConnectionState.lng(row, "DATA_AREA_LENGTH"),
                str(row, "DATA_AREA_VALUE"),
                str(row, "DATA_AREA_DESCRIPTION"));
    }

    @Override
    public CommandResult changeDataArea(String library, String name, String value) {
        // AS400-011 修复：library 参数必须经过 identifier 校验，防止 CL 命令注入
        String lib = library == null || library.isBlank() ? "QSYS" :
                JTOpenConnectionState.requireIdentifier(library, "库名");
        String n = JTOpenConnectionState.requireIdentifier(name, "数据区域名");
        String v = value == null ? "" : value.trim().replace("'", "''");
        return commandClient.execute("CHGDTAARA DTAARA(" + lib + "/" + n + ") VALUE('" + v + "')");
    }

    @Override
    public CommandResult createDataArea(String library, String name, int length, String value) {
        // AS400-011 修复
        String lib = library == null || library.isBlank() ? "QSYS" :
                JTOpenConnectionState.requireIdentifier(library, "库名");
        String n = JTOpenConnectionState.requireIdentifier(name, "数据区域名");
        int len = Math.max(1, Math.min(length, 2000));
        String v = value == null ? "" : value.trim().replace("'", "''");
        return commandClient.execute("CRTDTAARA DTAARA(" + lib + "/" + n + ") TYPE(*CHAR) LEN("
                + len + ") VALUE('" + v + "')");
    }

    @Override
    public CommandResult deleteDataArea(String library, String name) {
        // AS400-011 修复
        String lib = library == null || library.isBlank() ? "QSYS" :
                JTOpenConnectionState.requireIdentifier(library, "库名");
        String n = JTOpenConnectionState.requireIdentifier(name, "数据区域名");
        return commandClient.execute("DLTDTAARA DTAARA(" + lib + "/" + n + ")");
    }
}
