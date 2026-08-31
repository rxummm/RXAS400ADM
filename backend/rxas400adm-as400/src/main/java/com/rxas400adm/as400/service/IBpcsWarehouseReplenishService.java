package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsWarehouseReplenishQueryDTO;
import com.rxas400adm.as400.vo.BpcsWarehouseReplenishVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 多仓库联合补货查询（只读）。
 * 数据源 IWI + IIM + ITL + HPO，跨仓库库存可视与补货建议。
 */
public interface IBpcsWarehouseReplenishService {
    /** 按物料查询各仓库库存分布与补货建议 */
    PageResult<BpcsWarehouseReplenishVO> search(BpcsWarehouseReplenishQueryDTO query);
}
