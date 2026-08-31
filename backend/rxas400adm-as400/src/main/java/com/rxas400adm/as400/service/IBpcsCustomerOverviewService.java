package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsCustomerOverviewVO;

/**
 * 客户 360° 视图服务（⑳）。
 * 只读查询，数据源 BPCS RCM/ECH/EIN。
 */
public interface IBpcsCustomerOverviewService {

    /** 获取客户 360° 概览 */
    BpcsCustomerOverviewVO getOverview(String cono, String cust, int orderLimit, int invoiceLimit);
}
