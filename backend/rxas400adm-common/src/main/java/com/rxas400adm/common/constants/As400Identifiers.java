package com.rxas400adm.common.constants;

import java.util.regex.Pattern;

/**
 * S6（2026-08-25）：IBM i 标识符校验常量收敛——
 * 原正则在 JobService/BusinessService/CompileService/JTOpenConnectionState 四处独立复制，存在漂移风险。
 */
public final class As400Identifiers {

    /** IBM i 对象名合法字符集（大写字母/数字/下划线/$/#/@），注入面统一白名单 */
    public static final Pattern IDENTIFIER = Pattern.compile("^[A-Z0-9_$#@]+$");

    /** S6：作业号实际为 1~6 位十进制数字（IBM i 约定），单独精确校验 */
    public static final Pattern JOB_NUMBER = Pattern.compile("^\\d{1,6}$");

    private As400Identifiers() {
    }
}
