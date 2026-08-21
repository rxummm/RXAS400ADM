package com.rxas400adm.common.constants;

/**
 * SQL 方言（H2：dev 用 MySQL、生产/迁移目标 DB2 for i，LIMIT 等方言差异集中收敛）。
 */
public enum SqlDialect {

    /** MySQL 8（dev） */
    MYSQL {
        @Override
        public String limit(int n) {
            return "LIMIT " + n;
        }
    },

    /** DB2 for i（生产部署目标库） */
    DB2_I {
        @Override
        public String limit(int n) {
            return "FETCH FIRST " + n + " ROWS ONLY";
        }
    };

    /** 生成行数限制 SQL 片段（MySQL: LIMIT n；DB2 for i: FETCH FIRST n ROWS ONLY） */
    public abstract String limit(int n);
}