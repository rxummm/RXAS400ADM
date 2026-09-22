package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsKanbanVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ⑯ 订单看板视图接口。
 */
public interface IBpcsKanbanService {
    PageResult<BpcsKanbanVO> listKanbanOrders(String cono, int current, int size);
}
