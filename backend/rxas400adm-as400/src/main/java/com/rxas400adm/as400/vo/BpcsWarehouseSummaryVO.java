package com.rxas400adm.as400.vo;

/**
 * 仓库汇总 VO（BIN + WHS 聚合查询）：库位占用率。
 */
public record BpcsWarehouseSummaryVO(
        String whse,
        String whname,
        Integer totalBins,
        Integer occupied,
        Integer free,
        Integer totalCapacity
) {}
