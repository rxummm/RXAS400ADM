package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 库位主档 VO（BIN 表）。
 */
public record BpcsBinVO(
        String cono,
        String whse,
        String binno,
        String bintype,
        /** O=占用, F=空闲, R=保留, L=锁定 */
        String status,
        BigDecimal capacity
) {}
