package com.rxas400adm.as400.vo;

/**
 * 库存移动记录 VO（ITH 表）。
 */
public record BpcsMovementVO(
        String cono,
        String whse,
        String item,
        String frombin,
        String tobin,
        Integer qty,
        String ittyp,
        String trndate,
        String trntime,
        String refno,
        String userid
) {}
