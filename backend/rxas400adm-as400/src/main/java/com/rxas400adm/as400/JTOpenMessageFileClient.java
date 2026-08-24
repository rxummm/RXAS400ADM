package com.rxas400adm.as400;

import com.rxas400adm.as400.model.MessageDescriptor;
import com.rxas400adm.as400.model.MessageFileRow;
import com.rxas400adm.as400.model.MessageRow;

import java.util.ArrayList;
import java.util.List;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen MessageFileClient 委托实现（消息文件列表 / 消息描述增删改查）。
 */
class JTOpenMessageFileClient implements MessageFileClient {

    private final JTOpenSqlClient sqlClient;
    private final JTOpenCommandClient commandClient;

    JTOpenMessageFileClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
        this.commandClient = new JTOpenCommandClient(state);
    }

    @Override
    public List<MessageFileRow> listMessageFiles(String library) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        return sqlClient.queryList("SELECT MESSAGE_FILE_NAME, MESSAGE_FILE_LIBRARY, NUMBER_OF_MESSAGES, MESSAGE_FILE_TEXT "
                + "FROM QSYS2.MESSAGE_FILE_INFO WHERE MESSAGE_FILE_LIBRARY = ? "
                + "FETCH FIRST 200 ROWS ONLY", lib).stream()
                .map(r -> new MessageFileRow(str(r, "MESSAGE_FILE_NAME"), str(r, "MESSAGE_FILE_LIBRARY"),
                        lng(r, "NUMBER_OF_MESSAGES"), str(r, "MESSAGE_FILE_TEXT")))
                .toList();
    }

    @Override
    public List<MessageRow> listMessages(String library, String file, String keyword) {
        if (file == null || file.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        StringBuilder sql = new StringBuilder("SELECT MESSAGE_ID, MESSAGE_TEXT, SECOND_LEVEL_TEXT, SEVERITY ")
                .append("FROM QSYS2.MESSAGE_DESCRIPTION_INFO ")
                .append("WHERE MESSAGE_FILE_LIBRARY = ? AND MESSAGE_FILE_NAME = ?");
        List<Object> params = new ArrayList<>();
        params.add(lib);
        params.add(file.trim().toUpperCase());
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toUpperCase() + "%";
            sql.append(" AND (MESSAGE_ID LIKE ? OR MESSAGE_TEXT LIKE ?)");
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" FETCH FIRST 200 ROWS ONLY");
        return sqlClient.queryList(sql.toString(), params.toArray()).stream()
                .map(r -> new MessageRow(str(r, "MESSAGE_ID"), str(r, "MESSAGE_TEXT"),
                        str(r, "SECOND_LEVEL_TEXT"), lng(r, "SEVERITY")))
                .toList();
    }

    @Override
    public CommandResult addMessage(MessageDescriptor msg) {
        return messageDdlCommand("ADDMSGD", msg);
    }

    @Override
    public CommandResult updateMessage(MessageDescriptor msg) {
        return messageDdlCommand("CHGMSGD", msg);
    }

    /** ADDMSGD/CHGMSGD 共用拼装：仅动词不同，MSGID/MSGF/MSG/SECLVL/SEV 完全一致 */
    private CommandResult messageDdlCommand(String verb, MessageDescriptor msg) {
        String library = msg.library();
        String file = msg.file();
        String id = msg.id();
        String text = msg.text();
        String secondLevel = msg.secondLevel();
        int severity = msg.severity();
        if (file == null || file.isBlank() || id == null || id.isBlank()) {
            return CommandResult.fail("消息文件与消息 ID 不能为空");
        }
        String lib = library == null || library.isBlank() ? "QSYS" : JTOpenConnectionState.requireIdentifier(library, "库名");
        String cmd = verb + " MSGID(" + JTOpenConnectionState.requireIdentifier(id, "消息 ID") + ") MSGF(" + lib + "/"
                + JTOpenConnectionState.requireIdentifier(file, "消息文件") + ") MSG('" + text.replace("'", "''") + "')"
                + (secondLevel == null || secondLevel.isBlank() ? "" : " SECLVL('" + secondLevel.replace("'", "''") + "')")
                + " SEV(" + Math.max(0, Math.min(severity, 99)) + ")";
        return commandClient.execute(cmd);
    }

    @Override
    public CommandResult deleteMessage(String library, String file, String id) {
        if (file == null || file.isBlank() || id == null || id.isBlank()) {
            return CommandResult.fail("消息文件与消息 ID 不能为空");
        }
        String lib = library == null || library.isBlank() ? "QSYS" : JTOpenConnectionState.requireIdentifier(library, "库名");
        return commandClient.execute("RMVMSGD MSGID(" + JTOpenConnectionState.requireIdentifier(id, "消息 ID") + ") MSGF(" + lib + "/"
                + JTOpenConnectionState.requireIdentifier(file, "消息文件") + ")");
    }


}