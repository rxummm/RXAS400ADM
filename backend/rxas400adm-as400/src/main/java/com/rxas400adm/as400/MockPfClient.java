package com.rxas400adm.as400;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock PfClient 委托实现（物理文件列表 / 字段定义 / 记录分页查看）。
 */
class MockPfClient implements PfClient {

    MockPfClient(MockState state) {
    }

    @Override
    public List<PfRow> listPfFiles(String library) {
        String lib = library == null ? "APP" : library.toUpperCase();
        return List.of(
                new PfRow("CUSTMAST", lib, "客户主档"),
                new PfRow("ORDHDR", lib, "订单头"),
                new PfRow("ORDDTL", lib, "订单明细"),
                new PfRow("INVMAST", lib, "库存主档"));
    }

    @Override
    public List<PfColumnRow> pfColumns(String library, String file) {
        String f = file == null ? "" : file.toUpperCase();
        if (f.equals("CUSTMAST")) {
            return List.of(
                    new PfColumnRow("CUSTNO", "NUMERIC", 7, "N"),
                    new PfColumnRow("CUSTNAME", "CHARACTER", 30, "N"),
                    new PfColumnRow("BALANCE", "DECIMAL", 12, "Y"));
        }
        return List.of(
                new PfColumnRow("ORDNO", "NUMERIC", 7, "N"),
                new PfColumnRow("STATUS", "CHARACTER", 1, "N"));
    }

    @Override
    public List<Map<String, Object>> pfData(String library, String file, int limit) {
        String f = file == null ? "" : file.toUpperCase();
        int rows = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= rows; i++) {
            if (f.equals("CUSTMAST")) {
                data.add(MockState.row("CUSTNO", 10000 + i, "CUSTNAME", "客户" + (100 + i), "BALANCE", (i * 137) % 10000));
            } else {
                data.add(MockState.row("ORDNO", 9000 + i, "STATUS", i % 3 == 0 ? "C" : "O"));
            }
        }
        return data;
    }
}