package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsShipmentVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ㊳ 运单管理接口。
 */
public interface IBpcsShipmentMgmtService {

    /** 运单列表 */
    PageResult<BpcsShipmentVO> listShipments(String cono, int current, int size);
}
