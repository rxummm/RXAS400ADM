package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】物料主档查询入参。
 */
public class BpcsItemQueryDTO {

    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @Size(max = 15, message = "物料号最长 15 位")
    private String item;

    @Size(max = 30, message = "物料描述最长 30 位")
    private String desc;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
