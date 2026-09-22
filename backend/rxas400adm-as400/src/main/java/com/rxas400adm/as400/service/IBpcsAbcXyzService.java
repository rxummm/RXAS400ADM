package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsInventoryAbcXyzVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ABC/XYZ 矩阵分析服务（㊲）。
 * 只读查询，数据源 BPCS IWI/ITL/IIM。
 */
public interface IBpcsAbcXyzService {

    /** 获取 ABC/XYZ 矩阵分析结果 */
    PageResult<BpcsInventoryAbcXyzVO> getMatrix(String cono, String fromDate, int current, int size);
}
