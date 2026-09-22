package com.rxas400adm.as400.service;

import com.rxas400adm.as400.entity.OrderChange;
import com.rxas400adm.common.response.PageResult;

/**
 * 订单变更管理服务接口（只读查询）。
 */
public interface IOrderChangeService {
    PageResult<OrderChange> listByOrder(String cono, String orno, int current, int size);
}
