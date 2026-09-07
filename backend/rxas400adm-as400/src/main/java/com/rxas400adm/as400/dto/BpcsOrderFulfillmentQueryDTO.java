package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 订单履行率与 Backorder 查询入参。
 */
public class BpcsOrderFulfillmentQueryDTO {

    @Size(max = 3, message = "max length is 3")
    private String cono;

    @Size(max = 15, message = "max length is 15")
    private String itemFilter;

    private int current = 1;
    private int size = 50;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItemFilter() { return itemFilter; }
    public void setItemFilter(String itemFilter) { this.itemFilter = itemFilter; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
