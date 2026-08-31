package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsInventoryAbcXyzVO;

import java.util.List;

/**
 * ABC/XYZ 矩阵分析服务（㊲）。
 * 只读查询，数据源 BPCS IWI/ITL/IIM。
 */
public interface IBpcsAbcXyzService {

    /** 获取 ABC/XYZ 矩阵分析结果 */
    List<BpcsInventoryAbcXyzVO> getMatrix(String cono, String fromDate, int limit);
}
