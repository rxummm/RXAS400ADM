package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】库存可用量查询入参。
 */
public class BpcsInventoryQueryDTO {

    @Size(max = 3, message = "max length is 3")
    private String cono;

    @Size(max = 15, message = "max length is 15")
    private String item;

    @Size(max = 30, message = "max length is 30")
    private String desc;

    @Size(max = 4, message = "max length is 4")
    private String wh;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getWh() { return wh; }
    public void setWh(String wh) { this.wh = wh; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
