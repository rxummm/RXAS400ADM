package com.rxas400adm.common.constants;

/**
 * 执行状态常量（作业调度历史 / 脚本执行 / 报表定时 / 编译记录共用）。
 * 字符串值与 DB 存量数据一致，禁止改动；新代码引用常量而非散落字面量。
 */
public final class ExecutionStatus {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private ExecutionStatus() {
    }
}
