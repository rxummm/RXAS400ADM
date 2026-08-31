package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsStockValueVO;

import java.util.List;

/**
 * ㉚ 库存价值核算接口。
 */
public interface IBpcsStockValueService {

    /** 查询库存价值报表 */
    List<BpcsStockValueVO> getValueReport(String cono, int limit);
}
