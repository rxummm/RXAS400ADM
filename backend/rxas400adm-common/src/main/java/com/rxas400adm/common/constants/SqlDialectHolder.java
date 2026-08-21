package com.rxas400adm.common.constants;

/**
 * SQL 方言持有器（H2）：启动时由 app 模块按 {@code spring.datasource.url} 注入，
 * 默认 MySQL（dev/测试）。禁止直接依赖 —— 一律经 {@link PageConstants#limitClause(int)} 取方言化 SQL 片段。
 */
public final class SqlDialectHolder {

    private static volatile SqlDialect dialect = SqlDialect.MYSQL;

    private SqlDialectHolder() {
    }

    public static void set(SqlDialect d) {
        dialect = d == null ? SqlDialect.MYSQL : d;
    }

    public static SqlDialect get() {
        return dialect;
    }
}