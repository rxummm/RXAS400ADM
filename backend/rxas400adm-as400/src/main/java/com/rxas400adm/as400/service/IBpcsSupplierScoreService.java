package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsSupplierScoreVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ④ 供应商评分接口。
 */
public interface IBpcsSupplierScoreService {

    /** 查询供应商绩效评分 */
    PageResult<BpcsSupplierScoreVO> getSupplierScores(String cono, int current, int size);
}
