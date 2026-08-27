package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsShippingQueryDTO;
import com.rxas400adm.as400.vo.BpcsLoadVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】发运/载荷看板查询（只读）。
 */
public interface IBpcsShippingService {

    /** 按条件搜索载荷列表，支持分页 */
    PageResult<BpcsLoadVO> search(BpcsShippingQueryDTO query);
}
