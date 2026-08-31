package com.rxas400adm.as400.vo;

/**
 * 批次追踪 VO（IBL + IIM + ITL 联合查询）。
 */
public record BpcsBatchTrackingVO(
        String whse,
        String binno,
        String item,
        String itdsc,
        Integer qty,
        String lotno,
        String trndate,
        String ittyp,
        Integer trnqty,
        String refno
) {}
