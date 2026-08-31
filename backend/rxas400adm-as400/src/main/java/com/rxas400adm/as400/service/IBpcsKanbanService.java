package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsKanbanVO;

import java.util.List;

/**
 * ⑯ 订单看板视图接口。
 */
public interface IBpcsKanbanService {
    List<BpcsKanbanVO> listKanbanOrders(String cono, int limit);
}
