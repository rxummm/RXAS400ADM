package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsPurchaseQueryDTO;
import com.rxas400adm.as400.vo.BpcsPurchaseOrderVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】采购订单查询（只读）。
 */
public interface IBpcsPurchaseService {
    /** 按条件搜索采购订单（聚合 HPH + HPO），支持分页 */
    PageResult<BpcsPurchaseOrderVO> search(BpcsPurchaseQueryDTO query);
}
