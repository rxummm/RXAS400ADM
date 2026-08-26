package com.rxas400adm.common.constants;

/**
 * 分页公共常量（统一分页边界，防止散落的魔法值复制粘贴）。
 */
public final class PageConstants {

    private PageConstants() {
    }

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /** 默认每页条数 */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /** 每页条数上限（防超大查询拖垮数据库） */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 将分页参数夹到合法区间：[1, MAX_PAGE_SIZE] */
    public static long clampSize(long size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    /** 将页码夹到合法区间：[1, +∞) */
    public static long clampNum(long current) {
        return Math.max(1, current);
    }

    /**
     * 方言化行数限制 SQL 片段（H2：禁止再散落 {@code .last("LIMIT " + n)}）：
     * MySQL → {@code LIMIT n}；DB2 for i → {@code FETCH FIRST n ROWS ONLY}。
     * n 会被夹到 ≥ 1。
     */
    public static String limitClause(int n) {
        return SqlDialectHolder.get().limit(Math.max(1, n));
    }

    /**
     * B2：内存分页切片边界安全计算——long 运算防 (page-1)*size int 溢出，
     * 返回 [from, to)，调用方保证 from >= to 时不调 subList（或先判空）。
     */
    public static int[] sliceBounds(int current, int size, int total) {
        long fromL = (long) (Math.max(1, current) - 1) * size;
        int from = (int) Math.min(fromL, total);
        int to = (int) Math.min(fromL + Math.max(1, size), total);
        return new int[]{from, Math.max(from, to)};
    }
}