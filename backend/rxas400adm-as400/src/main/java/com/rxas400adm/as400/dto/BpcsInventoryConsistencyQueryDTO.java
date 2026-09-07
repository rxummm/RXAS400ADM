package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 库存多级一致性核对查询入参。
 */
public class BpcsInventoryConsistencyQueryDTO {

    @NotBlank(message = "company code is required")
    @Size(max = 3, message = "max length is 3")
    private String cono;

    @NotBlank(message = "item number is required")
    @Size(max = 15, message = "max length is 15")
    private String item;

    public String getCono() { return cono; }
    public void setCono(String cono) { this.cono = cono; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
}
