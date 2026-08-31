package com.rxas400adm.as400.vo;

/**
 * 仓库主档 VO（WHS 表）。
 */
public record BpcsWarehouseVO(
        String cono,
        String whse,
        String whname,
        String location
) {}
