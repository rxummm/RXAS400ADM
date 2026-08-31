package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import jakarta.validation.constraints.Size;

/**
 * 多仓库联合补货查询入参。
 * 数据源：IWI（仓库库存）+ IIM（物料主档）+ ITL（库存变动）。
 */
public class BpcsWarehouseReplenishQueryDTO {

    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @Size(max = 20, message = "物料号最长 20 位")
    private String item;

    @Size(max = 40, message = "物料描述最长 40 位")
    private String itdsc;

    /** 是否仅显示低于安全库存的物料 */
    private boolean belowSafetyOnly;

    private int current = 1;
    private int size = 20;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getItdsc() { return itdsc; }
    public void setItdsc(String itdsc) { this.itdsc = itdsc; }
    public boolean isBelowSafetyOnly() { return belowSafetyOnly; }
    public void setBelowSafetyOnly(boolean belowSafetyOnly) { this.belowSafetyOnly = belowSafetyOnly; }
    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public void setCurrent(int current) { this.current = current; }
    public int getSize() { return (int) PageConstants.clampSize(size); }
    public void setSize(int size) { this.size = size; }
}
