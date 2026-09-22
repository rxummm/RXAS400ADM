package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsStockValueVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ㉚ 库存价值核算接口。
 */
public interface IBpcsStockValueService {

    /** 查询库存价值报表 */
    PageResult<BpcsStockValueVO> getValueReport(String cono, int current, int size);
}
