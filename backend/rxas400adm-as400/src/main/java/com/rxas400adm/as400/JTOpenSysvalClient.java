package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SysvalRow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.rxas400adm.as400.JTOpenConnectionState.str;

/**
 * JTOpen SysvalClient 委托实现（系统值查询与修改）。
 */
class JTOpenSysvalClient implements SysvalClient {

    private final JTOpenSqlClient sqlClient;
    private final JTOpenCommandClient commandClient;

    JTOpenSysvalClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
        this.commandClient = new JTOpenCommandClient(state);
    }

    @Override
    public List<SysvalRow> listSystemValues() {
        return sqlClient.queryList("SELECT SYSTEM_VALUE_NAME, CURRENT_VALUE, VALUE_DESCRIPTION, SYSTEM_VALUE_TYPE "
                + "FROM QSYS2.SYSTEM_VALUE_INFO").stream()
                .map(r -> new SysvalRow(str(r, "SYSTEM_VALUE_NAME"), str(r, "CURRENT_VALUE"),
                        str(r, "VALUE_DESCRIPTION"), str(r, "SYSTEM_VALUE_TYPE")))
                .toList();
    }

    @Override
    public CommandResult changeSystemValue(String name, String value) {
        if (name == null || name.isBlank() || value == null) {
            return CommandResult.fail("系统值名称与值不能为空");
        }
        return commandClient.execute("CHGSYSVAL SYSVAL("
                + JTOpenConnectionState.requireIdentifier(name, "系统值名")
                + ") VALUE('" + value.trim().replace("'", "''") + "')");
    }

    @Override
    public Map<String, CommandResult> batchChangeSystemValues(Map<String, String> updates) {
        Map<String, CommandResult> results = new LinkedHashMap<>();
        if (updates == null || updates.isEmpty()) {
            return results;
        }
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            results.put(entry.getKey(), changeSystemValue(entry.getKey(), entry.getValue()));
        }
        return results;
    }

}