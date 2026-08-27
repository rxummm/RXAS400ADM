package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】发票轨迹查询入参。
 */
public class BpcsInvoiceQueryDTO {

    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @Size(max = 15, message = "订单号最长 15 位")
    private String orno;

    /** active / history */
    private String tab;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getOrno() { return orno; }
    public void setOrno(String orno) { this.orno = orno; }
    public String getTab() { return tab; }
    public void setTab(String tab) { this.tab = tab; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
