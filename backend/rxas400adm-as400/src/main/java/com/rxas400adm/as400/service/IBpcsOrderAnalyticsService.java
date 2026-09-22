package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsOrderFulfillmentQueryDTO;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.response.PageResult;

/**
 * 订单分析服务（履行率、OTD、Backorder）。
 * 只读查询，数据源 BPCS ECH/ECL/ESH。
 */
public interface IBpcsOrderAnalyticsService {

    /** 行级履行率统计 */
    BpcsOrderFulfillmentStatsVO getFulfillmentStats(String cono);

    /** Backorder 行明细 */
    PageResult<BpcsOrderBackorderLineVO> getBackorderLines(BpcsOrderFulfillmentQueryDTO query);

    /** Backorder 按物料聚合 */
    PageResult<BpcsOrderBackorderByItemVO> getBackorderByItem(String cono, int current, int size);

    /** OTD 统计 */
    BpcsOrderOtdStatsVO getOtdStats(String cono);

    /** OTD 按客户聚合 */
    PageResult<BpcsOrderOtdByCustomerVO> getOtdByCustomer(String cono, int current, int size);
}
