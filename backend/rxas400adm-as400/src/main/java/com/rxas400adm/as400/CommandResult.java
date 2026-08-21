package com.rxas400adm.as400;

/**
 * IBM i CL 命令执行结果
 */
public record CommandResult(boolean success, String message) {

    public static CommandResult ok(String message) {
        return new CommandResult(true, message);
    }

    public static CommandResult fail(String message) {
        return new CommandResult(false, message);
    }
}
