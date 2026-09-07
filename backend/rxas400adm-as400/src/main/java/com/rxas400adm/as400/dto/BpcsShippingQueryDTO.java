package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P2】发运看板查询入参。
 */
public class BpcsShippingQueryDTO {

    @Size(max = 3, message = "max length is 3")
    private String cono;

    @Size(max = 15, message = "max length is 15")
    private String lhno;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getLhno() { return lhno; }
    public void setLhno(String lhno) { this.lhno = lhno; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
