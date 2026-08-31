package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.*;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.response.PageResult;

import java.util.List;

/**
 * WMS 仓库管理服务接口。
 */
public interface IWmsService {

    /** 仓库主档查询 */
    PageResult<BpcsWarehouseVO> searchWarehouses(BpcsWarehouseQueryDTO dto);

    /** 库位主档查询 */
    PageResult<BpcsBinVO> searchBins(BpcsBinQueryDTO dto);

    /** 库位库存明细 */
    PageResult<BpcsBinInventoryVO> searchBinInventory(String cono, String whse, int current, int size);

    /** 库存移动记录 */
    PageResult<BpcsMovementVO> searchMovements(BpcsMovementQueryDTO dto);

    /** 批次追踪 */
    PageResult<BpcsBatchTrackingVO> searchBatches(BpcsBatchQueryDTO dto);

    /** 仓库汇总（库位占用率） */
    List<BpcsWarehouseSummaryVO> warehouseSummary(String cono);
}
