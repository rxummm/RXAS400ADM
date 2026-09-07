package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】客户档案查询入参。
 * 全部可选：支持按客户号/名称模糊搜索，公司码缺省走当前选中服务器。
 */
public class BpcsCustomerQueryDTO {

    @Size(max = 3, message = "max length is 3")
    private String cono;

    @Size(max = 6, message = "max length is 6")
    private String cust;

    @Size(max = 40, message = "max length is 40")
    private String name;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getCust() { return cust; }
    public void setCust(String cust) { this.cust = cust; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
