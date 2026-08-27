package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsInventoryQueryDTO;
import com.rxas400adm.as400.vo.BpcsInventoryVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】库存可用量查询（只读）。
 */
public interface IBpcsInventoryService {

    /** 按条件搜索库存可用量，支持分页 */
    PageResult<BpcsInventoryVO> search(BpcsInventoryQueryDTO query);
}
