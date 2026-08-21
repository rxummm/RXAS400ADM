package com.rxas400adm.as400;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mock 委托实现类共享的仿真状态（IFS 文件内存 / 子系统状态 / 系统值 / 消息文件）。
 */
class MockState {

    static final Random RANDOM = new Random();

    static final Map<String, String> MOCK_TABLE_TEXT = Map.of(
            "ORDERS", "销售订单主表", "SKU_MASTER", "商品档案", "PRICE_MASTER", "价格主档",
            "CUSTMAST", "客户主档", "EMPLOYEE", "员工档案");

    final String serverName;

    final ConcurrentMap<String, byte[]> mockIfsFiles = new ConcurrentHashMap<>();
    final java.util.Set<String> mockIfsDirs = ConcurrentHashMap.newKeySet();

    final ConcurrentMap<String, String> subsystemStatus = new ConcurrentHashMap<>(Map.of(
            "QSYS", "ACTIVE", "QINTER", "ACTIVE", "QBATCH", "ACTIVE",
            "QCMDQ", "ACTIVE", "QSPL", "ACTIVE", "QSYSPRT", "ACTIVE"));

    final ConcurrentMap<String, String> sysvalValues = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("QCCSID", "37"), Map.entry("QTIME", "14:30:00"), Map.entry("QDATE", "2026-08-13"),
            Map.entry("QHOUR", "24"), Map.entry("QDATFMT", "ISO"), Map.entry("QMAXSIGN", "5"),
            Map.entry("QINACTITV", "30"), Map.entry("QINACTMSGQ", "*ENDJOB"), Map.entry("QPWRDWNLMT", "30"),
            Map.entry("QALWOBJRST", "*ALL"), Map.entry("QJOBMSGQMX", "10"), Map.entry("QSTGLOWACN", "*MSG"),
            Map.entry("QSTGLOWLMT", "10"), Map.entry("QMLTTHDAC", "1"), Map.entry("QPFXPGM", "*NONE"),
            Map.entry("QCTLSBSD", "QCTL"), Map.entry("QFRCCVNRST", "0"), Map.entry("QSRLSTLST", "*NONE")));

    final ConcurrentMap<String, List<Map<String, Object>>> mockMessageStore = new ConcurrentHashMap<>();

    MockState(String serverName) {
        this.serverName = serverName;
    }

    String name() {
        return serverName;
    }

    List<Map<String, Object>> messagesOf(String library, String file) {
        return mockMessageStore.computeIfAbsent(msgKey(library, file), k -> seedMessages(k));
    }

    private String msgKey(String library, String file) {
        String lib = library == null || library.isBlank() ? "LIB" : library.trim();
        return (lib + "." + file.trim()).toUpperCase();
    }

    private List<Map<String, Object>> seedMessages(String key) {
        List<Map<String, Object>> seed = new ArrayList<>();
        if (key.equals("QUSRSYS.QUSRMSG")) {
            seed.add(row("MESSAGE_ID", "CPF0001", "MESSAGE_TEXT", "未处理的异常情况",
                    "SECOND_LEVEL_TEXT", "系统检测到未处理的异常情况，请联系系统管理员", "SEVERITY", 90L));
            seed.add(row("MESSAGE_ID", "CPF0002", "MESSAGE_TEXT", "对象未找到",
                    "SECOND_LEVEL_TEXT", "指定的对象在系统中不存在", "SEVERITY", 80L));
            seed.add(row("MESSAGE_ID", "CPF0003", "MESSAGE_TEXT", "权限不足",
                    "SECOND_LEVEL_TEXT", "用户没有执行该操作的权限", "SEVERITY", 70L));
        } else if (key.equals("APP.ORDMSG")) {
            seed.add(row("MESSAGE_ID", "ORD0001", "MESSAGE_TEXT", "订单创建成功",
                    "SECOND_LEVEL_TEXT", "订单已成功写入 ORDERS 表", "SEVERITY", 0L));
            seed.add(row("MESSAGE_ID", "ORD0002", "MESSAGE_TEXT", "订单已取消",
                    "SECOND_LEVEL_TEXT", "订单被人工取消，库存已回滚", "SEVERITY", 20L));
            seed.add(row("MESSAGE_ID", "ORD0003", "MESSAGE_TEXT", "库存不足",
                    "SECOND_LEVEL_TEXT", "订单行库存数量不足，无法发货", "SEVERITY", 50L));
        } else if (key.equals("APP.INVMSG")) {
            seed.add(row("MESSAGE_ID", "INV0001", "MESSAGE_TEXT", "盘点差异",
                    "SECOND_LEVEL_TEXT", "盘点数量与账面数量存在差异", "SEVERITY", 40L));
        } else {
            seed.add(row("MESSAGE_ID", "USR0001", "MESSAGE_TEXT", "通用提示消息",
                    "SECOND_LEVEL_TEXT", "演示用消息描述", "SEVERITY", 0L));
        }
        return seed;
    }

    static Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}