package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.OrderTemplateDTO;
import com.rxas400adm.as400.entity.OrderTemplate;
import com.rxas400adm.common.response.PageResult;

/**
 * 订单模板服务接口（CRUD）。
 */
public interface IOrderTemplateService {
    PageResult<OrderTemplate> list(String keyword, int current, int size);
    OrderTemplate get(Long id);
    OrderTemplate create(OrderTemplateDTO dto);
    OrderTemplate update(OrderTemplateDTO dto);
    void delete(Long id);
    OrderTemplate useTemplate(Long id);
}
