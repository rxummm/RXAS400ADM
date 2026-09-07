package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsTmsLiteVO;

import java.util.List;

/**
 * TMS Lite 运输管理服务接口。
 */
public interface IBpcsTmsLiteService {

    /** 路线规划列表 */
    List<BpcsTmsLiteVO.RoutePlan> getRoutePlans(String cono, int limit);

    /** 承运商比价 */
    List<BpcsTmsLiteVO.CarrierComparison> getCarrierComparison(String cono);

    /** 运费分析 */
    BpcsTmsLiteVO.FreightAnalysis getFreightAnalysis(String cono, int months);

    /** 签收追踪 */
    List<BpcsTmsLiteVO.DeliveryTracking> getDeliveryTracking(String cono, String status, int limit);

    /** 综合数据 */
    BpcsTmsLiteVO getAll(String cono, int months);
}
