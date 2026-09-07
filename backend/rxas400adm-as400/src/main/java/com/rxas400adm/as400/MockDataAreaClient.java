package com.rxas400adm.as400;

import com.rxas400adm.as400.model.DataAreaRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock DataAreaClient 委托实现（数据区域查询与修改）。
 */
class MockDataAreaClient implements DataAreaClient {

    private final Map<String, String> dataAreas = new ConcurrentHashMap<>();

    MockDataAreaClient(MockState state) {
        // 初始化仿真数据
        dataAreas.put("QSYS/RXCONFIG", "{\"app\":\"rxas400\",\"version\":\"1.0\"}");
        dataAreas.put("QSYS/RXSESSION", "ACTIVE");
        dataAreas.put("QSYS/RXVERSION", "1.0.0");
        dataAreas.put("QUSRSYS/RXUSER", "ADMIN");
    }

    @Override
    public List<DataAreaRow> listDataAreas(String library) {
        String lib = library == null ? "QSYS" : library.trim().toUpperCase();
        List<DataAreaRow> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : dataAreas.entrySet()) {
            String[] parts = entry.getKey().split("/", 2);
            if (parts[0].equals(lib)) {
                result.add(new DataAreaRow(parts[0], parts[1], "*CHAR",
                        entry.getValue().length(), entry.getValue(), "Mock data area"));
            }
        }
        return result;
    }

    @Override
    public DataAreaRow getDataArea(String library, String name) {
        String lib = library == null ? "QSYS" : library.trim().toUpperCase();
        String n = name == null ? "" : name.trim().toUpperCase();
        String key = lib + "/" + n;
        String value = dataAreas.get(key);
        if (value == null) {
            return null;
        }
        return new DataAreaRow(lib, n, "*CHAR", value.length(), value, "Mock data area");
    }

    @Override
    public CommandResult changeDataArea(String library, String name, String value) {
        String lib = library == null ? "QSYS" : library.trim().toUpperCase();
        String n = name == null ? "" : name.trim().toUpperCase();
        String key = lib + "/" + n;
        if (!dataAreas.containsKey(key)) {
            return CommandResult.fail("数据区域不存在: " + key);
        }
        dataAreas.put(key, value == null ? "" : value.trim());
        return CommandResult.ok("[Mock] 数据区域已修改: " + key);
    }

    @Override
    public CommandResult createDataArea(String library, String name, int length, String value) {
        String lib = library == null ? "QSYS" : library.trim().toUpperCase();
        String n = name == null ? "" : name.trim().toUpperCase();
        String key = lib + "/" + n;
        if (dataAreas.containsKey(key)) {
            return CommandResult.fail("数据区域已存在: " + key);
        }
        dataAreas.put(key, value == null ? "" : value.trim());
        return CommandResult.ok("[Mock] 数据区域已创建: " + key);
    }

    @Override
    public CommandResult deleteDataArea(String library, String name) {
        String lib = library == null ? "QSYS" : library.trim().toUpperCase();
        String n = name == null ? "" : name.trim().toUpperCase();
        String key = lib + "/" + n;
        if (dataAreas.remove(key) == null) {
            return CommandResult.fail("数据区域不存在: " + key);
        }
        return CommandResult.ok("[Mock] 数据区域已删除: " + key);
    }
}
