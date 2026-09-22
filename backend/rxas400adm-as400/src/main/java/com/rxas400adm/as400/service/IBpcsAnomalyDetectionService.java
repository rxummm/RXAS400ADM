package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsOrderAnomalyVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ⑨ 异常检测引擎接口。
 */
public interface IBpcsAnomalyDetectionService {

    /** 扫描异常订单（Hold/Backorder/延迟） */
    PageResult<BpcsOrderAnomalyVO> detectAnomalies(String cono, int current, int size);
}
