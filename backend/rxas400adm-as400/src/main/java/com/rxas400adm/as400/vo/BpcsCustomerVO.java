package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】客户档案 VO（RCM 主档 + EST 收货点）。
 */
public record BpcsCustomerVO(
        String cono,
        String cust,
        String name,
        String address1,
        String address2,
        String city,
        String state,
        String zip,
        String phone,
        String contact,
        /** 信用额度（各环境差异大，null 表示未维护） */
        BigDecimal creditLimit,
        /** 付款条件码（如 N30=净30天） */
        String termsCode,
        /** 税务码 */
        String taxCode,
        /** 销售区域 */
        String salesArea,
        /** 收货点列表 */
        List<ShipToVO> shipTos
) {
    public record ShipToVO(
            String ship,
            String name,
            String address1,
            String city,
            String state,
            String zip,
            String phone
    ) {}
}
