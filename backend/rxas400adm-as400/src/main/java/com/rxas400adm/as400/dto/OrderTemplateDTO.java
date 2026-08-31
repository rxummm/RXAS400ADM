package com.rxas400adm.as400.dto;

import lombok.Data;

/**
 * 订单模板创建/更新 DTO。
 */
@Data
public class OrderTemplateDTO {
    private Long id;
    private String templateName;
    private String cono;
    private String cust;
    private String shipTo;
    private String remark;
    private String lineJson;
}
