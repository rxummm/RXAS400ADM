package com.rxas400adm.as400;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock ObjectClient 委托实现（对象搜索 / 详情 / 引用 / 权限 / 拓扑）。
 */
class MockObjectClient implements ObjectClient {

    MockObjectClient(MockState state) {
    }

    @Override
    public boolean objectExists(String library, String object) {
        return library != null && !library.isBlank() && !library.toUpperCase().startsWith("Q");
    }

    @Override
    public List<ObjectRow> searchObjects(String library, String objectType) {
        String[] types = {"PGM", "SRVPGM", "MODULE", "FILE", "*DTAQ", "MSGF"};
        String[] names = {"ORDERMAINT", "CUSTMAINT", "INVMOVE", "DAILYBAL", "PAYROLL", "REPORT001"};
        String typeFilter = objectType == null ? "" : objectType.trim().toUpperCase();
        String lib = library == null ? "APP" : library.toUpperCase();
        List<ObjectRow> rows = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String type = types[i % types.length];
            if (!typeFilter.isEmpty() && !typeFilter.equals(type)) {
                continue;
            }
            rows.add(new ObjectRow(names[i], type, lib,
                    (MockState.RANDOM.nextInt(4000) + 100) * 1024L,
                    "2026-0" + (1 + MockState.RANDOM.nextInt(8)) + "-12 09:00:00"));
        }
        return rows;
    }

    @Override
    public ObjectDetail objectDetail(String library, String objectName) {
        String lib = library == null ? "APP" : library.toUpperCase();
        String name = objectName == null ? "" : objectName.toUpperCase();
        if (name.isBlank()) {
            return null;
        }
        return new ObjectDetail(name, "PGM", lib,
                (MockState.RANDOM.nextInt(4000) + 100) * 1024L,
                "2026-03-12 09:00:00",
                "2026-07-2" + MockState.RANDOM.nextInt(9) + " 14:30:00",
                "模拟对象: " + name + "（" + lib + "）",
                "QSECOFR",
                "SYSBAS");
    }

    @Override
    public List<ObjectRefRow> objectReferences(String library, String objectName, String direction) {
        String lib = library == null ? "APP" : library.toUpperCase();
        String name = objectName == null ? "" : objectName.toUpperCase();
        boolean inbound = direction != null && "IN".equalsIgnoreCase(direction);
        List<ObjectRefRow> rows = new ArrayList<>();
        if (inbound) {
            String[][] refs = {
                    {"DAILYBAL", "PGM", "BALCTL"},
                    {"PAYROLL", "PGM", "EMPCTL"},
                    {"CUSTMAINT", "PGM", name}
            };
            for (String[] r : refs) {
                rows.add(new ObjectRefRow(lib, r[0], r[1], lib, r[2], "*PGM"));
            }
        } else {
            String[][] refs = {
                    {"INVCTL", "PGM"},
                    {"ORDERFILE", "FILE"},
                    {"CUSTFILE", "FILE"},
                    {"MSGDTA", "MSGF"}
            };
            for (String[] r : refs) {
                rows.add(new ObjectRefRow(lib, name, "PGM", lib, r[0], r[1]));
            }
        }
        return rows;
    }

    @Override
    public List<AuthorityRow> objectAuthorities(String library, String objectName) {
        String name = objectName == null ? "" : objectName.toUpperCase();
        if (name.isBlank()) {
            return List.of();
        }
        return List.of(
                new AuthorityRow("QSECOFR", "*USER", "*ALL"),
                new AuthorityRow("GRPDEV", "*GROUP", "*CHANGE"),
                new AuthorityRow("GRPOPR", "*GROUP", "*USE"),
                new AuthorityRow("*PUBLIC", "*PUBLIC", "*EXCLUDE"));
    }

    @Override
    public GraphData objectGraph(String library) {
        String lib = library == null || library.isBlank() ? "APP" : library.toUpperCase();
        String[][] nodes = {
                {"ORDERMAINT", "PGM"}, {"CUSTMAINT", "PGM"}, {"INVMOVE", "PGM"},
                {"DAILYBAL", "PGM"}, {"PAYROLL", "PGM"},
                {"ORDCTL", "PGM"}, {"CUSTCTL", "PGM"}, {"INVCTL", "PGM"}, {"BALCTL", "PGM"}, {"EMPCTL", "PGM"},
                {"ORDFILE", "FILE"}, {"CUSTFILE", "FILE"}, {"INVFILE", "FILE"}, {"BALFILE", "FILE"}, {"EMPFILE", "FILE"},
                {"MSGDTA", "MSGF"}
        };
        String[][] links = {
                {"ORDERMAINT", "ORDCTL"}, {"ORDERMAINT", "ORDFILE"}, {"ORDERMAINT", "CUSTFILE"}, {"ORDERMAINT", "MSGDTA"},
                {"ORDCTL", "ORDFILE"}, {"ORDCTL", "MSGDTA"},
                {"CUSTMAINT", "CUSTCTL"}, {"CUSTMAINT", "CUSTFILE"},
                {"CUSTCTL", "CUSTFILE"}, {"CUSTCTL", "MSGDTA"},
                {"INVMOVE", "INVCTL"}, {"INVMOVE", "INVFILE"},
                {"INVCTL", "INVFILE"}, {"INVCTL", "MSGDTA"},
                {"DAILYBAL", "BALCTL"}, {"DAILYBAL", "BALFILE"}, {"DAILYBAL", "MSGDTA"},
                {"BALCTL", "BALFILE"},
                {"PAYROLL", "EMPCTL"}, {"PAYROLL", "EMPFILE"}, {"PAYROLL", "MSGDTA"},
                {"EMPCTL", "EMPFILE"}
        };
        List<GraphNode> nodeRows = new ArrayList<>();
        for (String[] n : nodes) {
            nodeRows.add(new GraphNode(lib + "." + n[0], n[0], n[1], lib));
        }
        List<GraphLink> linkRows = new ArrayList<>();
        for (String[] l : links) {
            linkRows.add(new GraphLink(lib + "." + l[0], lib + "." + l[1]));
        }
        return new GraphData(nodeRows, linkRows);
    }
}