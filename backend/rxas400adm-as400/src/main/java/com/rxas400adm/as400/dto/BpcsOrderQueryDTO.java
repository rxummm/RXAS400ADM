package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P1】客户订单查询入参。
 */
public class BpcsOrderQueryDTO {

    @NotBlank(message = "company code is required")
    @Size(max = 3, message = "max length is 3")
    private String cono;

    @NotBlank(message = "order number is required")
    @Size(max = 16, message = "max length is 16")
    private String orno;

    public String getCono() {
        return cono;
    }

    public void setCono(String cono) {
        this.cono = cono;
    }

    public String getOrno() {
        return orno;
    }

    public void setOrno(String orno) {
        this.orno = orno;
    }
}
