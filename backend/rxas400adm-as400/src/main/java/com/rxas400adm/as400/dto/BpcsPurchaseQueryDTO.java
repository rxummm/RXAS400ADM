package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】采购订单查询入参。
 * 数据源：HPH（采购订单头）+ HPO（采购订单行）。
 */
public class BpcsPurchaseQueryDTO {

    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @Size(max = 16, message = "采购单号最长 16 位")
    private String pono;

    @Size(max = 6, message = "供应商号最长 6 位")
    private String vendor;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getPono() { return pono; }
    public void setPono(String pono) { this.pono = pono; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
