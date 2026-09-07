package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】销售趋势查询入参。
 * 按月统计 SSH/SSD 数据。
 */
public class BpcsSalesQueryDTO {

    @Size(max = 3, message = "max length is 3")
    private String cono;

    /** 起始年月 YYYYMM（可选） */
    private String fromYm;

    /** 结束年月 YYYYMM（可选） */
    private String toYm;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getFromYm() { return fromYm; }
    public void setFromYm(String fromYm) { this.fromYm = fromYm; }
    public String getToYm() { return toYm; }
    public void setToYm(String toYm) { this.toYm = toYm; }
}
