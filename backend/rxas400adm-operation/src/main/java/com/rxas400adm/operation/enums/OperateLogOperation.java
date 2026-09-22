package com.rxas400adm.operation.enums;

/**
 * 审计日志操作类型枚举。
 * 用于 @OperateLog 注解的 operation 参数。
 */
public enum OperateLogOperation {
    VIEW("view", "查看"),
    CREATE("create", "创建"),
    UPDATE("update", "更新"),
    DELETE("delete", "删除"),
    EXPORT("export", "导出"),
    IMPORT("import", "导入"),
    APPROVE("approve", "审批"),
    REJECT("reject", "驳回"),
    SUBMIT("submit", "提交"),
    EXECUTE("execute", "执行"),
    RUN("run", "运行"),
    CALCULATE("calculate", "计算"),
    SEND("send", "发送"),
    RECEIVE("receive", "接收");

    private final String code;
    private final String label;

    OperateLogOperation(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
