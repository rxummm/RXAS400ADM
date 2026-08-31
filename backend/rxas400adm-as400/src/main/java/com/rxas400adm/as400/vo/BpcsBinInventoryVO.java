package com.rxas400adm.as400.vo;

/**
 * 库位库存明细 VO（BIN + IBL + IIM 联合查询）。
 */
public record BpcsBinInventoryVO(
        String whse,
        String binno,
        String bintype,
        String status,
        Integer capacity,
        String item,
        String itdsc,
        Integer qty,
        String lotno
) {}
