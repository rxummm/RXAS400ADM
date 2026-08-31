package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户 360° 视图 VO（⑳ 客户 360° 视图）。
 * 聚合 RCM（客户主档）+ ECH（订单）+ EIN（发票）。
 */
public record BpcsCustomerOverviewVO(
        String cono,
        String cust,
        String customerName,
        String address,
        String city,
        String state,
        String zip,
        String phone,
        String contact,
        /** 信用额度 */
        BigDecimal creditLimit,
        /** 付款条件 */
        String termsCode,
        /** 税务码 */
        String taxCode,
        /** 销售员 */
        String salesRep,
        /** 历史总订单数 */
        int totalOrders,
        /** 当前未结订单数 */
        int openOrders,
        /** 历史总收入 */
        BigDecimal totalRevenue,
        /** 最近订单列表 */
        List<RecentOrder> recentOrders,
        /** 逾期发票列表 */
        List<OverdueInvoice> overdueInvoices
) {
    /** 最近订单 */
    public record RecentOrder(
            String orderNo,
            String orderDate,
            String reqDate,
            String headerStatus,
            int lineCount,
            BigDecimal orderTotal
    ) {}

    /** 逾期发票 */
    public record OverdueInvoice(
            String invoiceNo,
            String invoiceDate,
            BigDecimal invoiceAmount,
            String orderNo
    ) {}
}
