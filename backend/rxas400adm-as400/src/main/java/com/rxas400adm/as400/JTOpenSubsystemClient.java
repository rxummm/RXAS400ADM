package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SubsystemRow;

import java.util.List;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen SubsystemClient 委托实现（子系统状态与启停）。
 */
class JTOpenSubsystemClient implements SubsystemClient {

    private final JTOpenSqlClient sqlClient;
    private final JTOpenCommandClient commandClient;

    JTOpenSubsystemClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
        this.commandClient = new JTOpenCommandClient(state);
    }

    @Override
    public List<SubsystemRow> listSubsystems() {
        return sqlClient.queryList("SELECT SUBSYSTEM_NAME, SUBSYSTEM_DESCRIPTION, STATUS, "
                + "NUMBER_OF_ACTIVE_JOBS, MAXIMUM_ACTIVE_JOBS, SUBSYSTEM_LIBRARY "
                + "FROM QSYS2.SUBSYSTEM_INFO").stream()
                .map(r -> new SubsystemRow(str(r, "SUBSYSTEM_NAME"), str(r, "SUBSYSTEM_DESCRIPTION"),
                        str(r, "STATUS"), lng(r, "NUMBER_OF_ACTIVE_JOBS"),
                        lng(r, "MAXIMUM_ACTIVE_JOBS"), str(r, "SUBSYSTEM_LIBRARY")))
                .toList();
    }

    @Override
    public CommandResult startSubsystem(String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.fail("子系统名不能为空");
        }
        return commandClient.execute("STR subsystem "
                + JTOpenConnectionState.requireIdentifier(name, "子系统名"));
    }

    @Override
    public CommandResult endSubsystem(String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.fail("子系统名不能为空");
        }
        return commandClient.execute("END subsystem "
                + JTOpenConnectionState.requireIdentifier(name, "子系统名") + " OPTION(*IMMED)");
    }


}