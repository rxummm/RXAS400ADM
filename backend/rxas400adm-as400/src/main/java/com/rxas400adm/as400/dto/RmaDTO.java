package com.rxas400adm.as400.dto;

import lombok.Data;

/**
 * 退货 RMA 创建 DTO。
 */
@Data
public class RmaDTO {
    private String cono;
    private String orno;
    private String cust;
    private String item;
    private Integer qty;
    private String reason;
}
