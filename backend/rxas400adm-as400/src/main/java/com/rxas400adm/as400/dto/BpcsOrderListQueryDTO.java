package com.rxas400adm.as400.dto;

import lombok.Data;

/**
 * 订单列表搜索查询参数。
 */
@Data
public class BpcsOrderListQueryDTO {
    /** 公司码 */
    private String cono = "001";
    /** 订单号（模糊） */
    private String orno;
    /** 客户号（模糊） */
    private String cust;
    /** 起始日期 YYYYMMDD */
    private String fromDate;
    /** 结束日期 YYYYMMDD */
    private String toDate;
    /** 当前页 */
    private int current = 1;
    /** 每页条数 */
    private int size = 20;
}
