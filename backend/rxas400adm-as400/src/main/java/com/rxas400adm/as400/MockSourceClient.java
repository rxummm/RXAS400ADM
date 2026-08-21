package com.rxas400adm.as400;

import java.util.List;

/**
 * Mock SourceClient 委托实现（源码库/源文件/成员仿真）。
 */
class MockSourceClient implements SourceClient {

    MockSourceClient() {
    }

    @Override
    public List<String> listLibraries() {
        return List.of("RXAS400", "APP", "ORDERS", "INVENTORY", "QRPGLESRC", "QTEMP");
    }

    @Override
    public List<String> listSourceFiles(String library) {
        return List.of("QRPGLESRC", "QCLSRC", "QSQLSRC", "QDDSSRC");
    }

    @Override
    public List<String> listMembers(String library, String sourceFile) {
        return switch (sourceFile.toUpperCase()) {
            case "QRPGLESRC" -> List.of("CUSTMAINT", "ORDERMAINT", "INVMOVE", "DAILYBAL");
            case "QCLSRC" -> List.of("STARTUP", "SHUTDOWN", "BKUPCTL");
            case "QSQLSRC" -> List.of("SELORDERS", "UPDSTOCK");
            default -> List.of("DDSFILE01", "DDSFILE02");
        };
    }

    @Override
    public String readMember(String library, String sourceFile, String member) {
        return "      * ============================================================\n"
                + "      * " + library + "/" + sourceFile + "/" + member + " (模拟内容)\n"
                + "      * ============================================================\n"
                + "     H DFTACTGRP(*NO) ACTGRP('CUST') BNDDIR('QC2LE')\n"
                + "     D CUSTNO          S             10A\n"
                + "     C     *ENTRY        PLIST\n"
                + "     C                   PARM           CUSTNO\n"
                + "     C                   EVAL      CUSTNO = %TRIM(CUSTNO)\n"
                + "     C                   RETURN\n";
    }
}