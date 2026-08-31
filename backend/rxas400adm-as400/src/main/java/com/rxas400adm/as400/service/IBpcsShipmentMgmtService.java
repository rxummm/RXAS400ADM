package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsShipmentVO;

import java.util.List;

/**
 * ㊳ 运单管理接口。
 */
public interface IBpcsShipmentMgmtService {

    /** 运单列表 */
    List<BpcsShipmentVO> listShipments(String cono, int limit);
}
