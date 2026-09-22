package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsReplenishmentVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ② 智能补货建议接口。
 */
public interface IBpcsReplenishmentService {

    /** 查询低于安全库存的物料 */
    PageResult<BpcsReplenishmentVO> getReplenishmentSuggestions(String cono, int current, int size);
}
