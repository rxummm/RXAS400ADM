package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsOrderAnomalyVO;

import java.util.List;

/**
 * ⑨ 异常检测引擎接口。
 */
public interface IBpcsAnomalyDetectionService {

    /** 扫描异常订单（Hold/Backorder/延迟） */
    List<BpcsOrderAnomalyVO> detectAnomalies(String cono, int limit);
}
