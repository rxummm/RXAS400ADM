package com.rxas400adm.as400.dto;

import lombok.Data;

/**
 * 订单排程创建/更新 DTO。
 */
@Data
public class OrderScheduleDTO {
    private String cono;
    private String orno;
    private String cust;
    private String startDate;
    private String endDate;
    private Integer progress;
    private Integer priority;
}
