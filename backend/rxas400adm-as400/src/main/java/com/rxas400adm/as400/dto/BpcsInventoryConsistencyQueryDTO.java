package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 库存多级一致性核对查询入参。
 */
public class BpcsInventoryConsistencyQueryDTO {

    @NotBlank(message = "公司码不能为空")
    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @NotBlank(message = "物料号不能为空")
    @Size(max = 15, message = "物料号最长 15 位")
    private String item;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
}
