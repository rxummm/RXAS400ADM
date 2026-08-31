package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsSupplierScoreVO;

import java.util.List;

/**
 * ④ 供应商评分接口。
 */
public interface IBpcsSupplierScoreService {

    /** 查询供应商绩效评分 */
    List<BpcsSupplierScoreVO> getSupplierScores(String cono, int limit);
}
