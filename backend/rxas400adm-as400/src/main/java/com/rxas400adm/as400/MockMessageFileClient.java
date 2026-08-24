package com.rxas400adm.as400;

import com.rxas400adm.as400.model.MessageDescriptor;
import com.rxas400adm.as400.model.MessageFileRow;
import com.rxas400adm.as400.model.MessageRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock MessageFileClient 委托实现（消息文件列表 / 消息描述增删改查）。
 */
class MockMessageFileClient implements MessageFileClient {

    private final MockState state;

    MockMessageFileClient(MockState state) {
        this.state = state;
    }

    @Override
    public List<MessageFileRow> listMessageFiles(String library) {
        String[][] files = {
                {"QUSRMSG", "QUSRSYS", "系统用户消息文件"},
                {"QCPFMSG", "QSYS", "控制程序消息文件"},
                {"ORDMSG", "APP", "订单业务消息文件"},
                {"INVMSG", "APP", "库存业务消息文件"}
        };
        List<MessageFileRow> rows = new ArrayList<>();
        for (String[] f : files) {
            if (library == null || library.isBlank() || library.trim().equalsIgnoreCase(f[1])) {
                rows.add(new MessageFileRow(f[0], f[1], state.messagesOf(f[1], f[0]).size(), f[2]));
            }
        }
        return rows;
    }

    @Override
    public List<MessageRow> listMessages(String library, String file, String keyword) {
        if (file == null || file.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> all = new ArrayList<>(state.messagesOf(library, file));
        if (keyword == null || keyword.isBlank()) {
            return all.stream().map(m -> new MessageRow(
                    String.valueOf(m.get("MESSAGE_ID")),
                    String.valueOf(m.getOrDefault("MESSAGE_TEXT", "")),
                    String.valueOf(m.getOrDefault("SECOND_LEVEL_TEXT", "")),
                    toLong(m.get("SEVERITY")))).toList();
        }
        String kw = keyword.toUpperCase();
        List<MessageRow> filtered = new ArrayList<>();
        for (Map<String, Object> m : all) {
            if (String.valueOf(m.get("MESSAGE_ID")).toUpperCase().contains(kw)
                    || String.valueOf(m.get("MESSAGE_TEXT")).toUpperCase().contains(kw)) {
                filtered.add(new MessageRow(
                        String.valueOf(m.get("MESSAGE_ID")),
                        String.valueOf(m.getOrDefault("MESSAGE_TEXT", "")),
                        String.valueOf(m.getOrDefault("SECOND_LEVEL_TEXT", "")),
                        toLong(m.get("SEVERITY"))));
            }
        }
        return filtered;
    }

    @Override
    public CommandResult addMessage(MessageDescriptor msg) {
        String library = msg.library(); String file = msg.file(); String id = msg.id();
        String text = msg.text(); String secondLevel = msg.secondLevel(); int severity = msg.severity();
        if (file == null || file.isBlank() || id == null || id.isBlank()) {
            return CommandResult.fail("消息文件与消息 ID 不能为空");
        }
        String upperId = id.trim().toUpperCase();
        List<Map<String, Object>> list = state.messagesOf(library, file);
        if (list.stream().anyMatch(m -> String.valueOf(m.get("MESSAGE_ID")).equals(upperId))) {
            return CommandResult.fail("消息 ID " + upperId + " 已存在");
        }
        list.add(MockState.row("MESSAGE_ID", upperId, "MESSAGE_TEXT", text,
                "SECOND_LEVEL_TEXT", secondLevel, "SEVERITY", (long) severity));
        return CommandResult.ok("模拟新增消息 " + upperId);
    }

    @Override
    public CommandResult updateMessage(MessageDescriptor msg) {
        String library = msg.library(); String file = msg.file(); String id = msg.id();
        String text = msg.text(); String secondLevel = msg.secondLevel(); int severity = msg.severity();
        if (file == null || file.isBlank() || id == null || id.isBlank()) {
            return CommandResult.fail("消息文件与消息 ID 不能为空");
        }
        String upperId = id.trim().toUpperCase();
        List<Map<String, Object>> list = state.messagesOf(library, file);
        for (Map<String, Object> m : list) {
            if (String.valueOf(m.get("MESSAGE_ID")).equals(upperId)) {
                m.put("MESSAGE_TEXT", text);
                m.put("SECOND_LEVEL_TEXT", secondLevel);
                m.put("SEVERITY", (long) severity);
                return CommandResult.ok("模拟修改消息 " + upperId);
            }
        }
        return CommandResult.fail("消息 ID " + upperId + " 不存在");
    }

    @Override
    public CommandResult deleteMessage(String library, String file, String id) {
        if (file == null || file.isBlank() || id == null || id.isBlank()) {
            return CommandResult.fail("消息文件与消息 ID 不能为空");
        }
        String upperId = id.trim().toUpperCase();
        List<Map<String, Object>> list = state.messagesOf(library, file);
        boolean removed = list.removeIf(m -> String.valueOf(m.get("MESSAGE_ID")).equals(upperId));
        return removed ? CommandResult.ok("模拟删除消息 " + upperId)
                : CommandResult.fail("消息 ID " + upperId + " 不存在");
    }

    private long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}