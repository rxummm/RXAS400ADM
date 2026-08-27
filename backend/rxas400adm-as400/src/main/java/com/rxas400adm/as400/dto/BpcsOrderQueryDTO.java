package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 【AS400 业务增强·P1】客户订单查询入参。
 */
public class BpcsOrderQueryDTO {

    @NotBlank(message = "公司码不能为空")
    @Size(max = 3, message = "公司码最长 3 位")
    private String cono;

    @NotBlank(message = "订单号不能为空")
    @Size(max = 16, message = "订单号最长 16 位")
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
