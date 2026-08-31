package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsLocationInventoryVO;

import java.util.List;

/**
 * ㉗ 库位库存可视化接口。
 */
public interface IBpcsLocationService {

    /** 查询库位库存列表 */
    List<BpcsLocationInventoryVO> listLocationInventory(String cono, int limit);
}
