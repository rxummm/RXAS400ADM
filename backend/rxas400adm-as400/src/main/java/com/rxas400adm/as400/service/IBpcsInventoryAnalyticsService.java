package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsInventoryConsistencyVO;
import com.rxas400adm.as400.vo.BpcsInventorySlowMovingVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 库存分析服务（多级一致性核对、呆滞物料分析）。
 * 只读查询，数据源 BPCS IPI/ILI/IWM/IWI/IIM/ITL。
 */
public interface IBpcsInventoryAnalyticsService {

    /** 库存多级一致性核对 */
    BpcsInventoryConsistencyVO checkConsistency(String cono, String item);

    /** 呆滞物料分析 */
    PageResult<BpcsInventorySlowMovingVO> getSlowMovingItems(String cono, String cutoffDate, int current, int size);
}
