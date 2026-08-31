package com.rxas400adm.as400.service;

import com.rxas400adm.as400.entity.OrderChange;

import java.util.List;

/**
 * 订单变更管理服务接口（只读查询）。
 */
public interface IOrderChangeService {
    List<OrderChange> listByOrder(String cono, String orno);
}
