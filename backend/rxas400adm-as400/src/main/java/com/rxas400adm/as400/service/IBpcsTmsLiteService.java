package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsTmsLiteVO;
import com.rxas400adm.common.response.PageResult;

/**
 * TMS Lite 运输管理服务接口。
 */
public interface IBpcsTmsLiteService {

    /** 路线规划列表 */
    PageResult<BpcsTmsLiteVO.RoutePlan> getRoutePlans(String cono, int current, int size);

    /** 承运商比价 */
    PageResult<BpcsTmsLiteVO.CarrierComparison> getCarrierComparison(String cono, int current, int size);

    /** 运费分析 */
    BpcsTmsLiteVO.FreightAnalysis getFreightAnalysis(String cono, int months);

    /** 签收追踪 */
    PageResult<BpcsTmsLiteVO.DeliveryTracking> getDeliveryTracking(String cono, String status, int current, int size);

    /** 综合数据 */
    BpcsTmsLiteVO getAll(String cono, int months);
}
