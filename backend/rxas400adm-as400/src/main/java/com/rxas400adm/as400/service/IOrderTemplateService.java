package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.OrderTemplateDTO;
import com.rxas400adm.as400.entity.OrderTemplate;

import java.util.List;

/**
 * 订单模板服务接口（CRUD）。
 */
public interface IOrderTemplateService {
    List<OrderTemplate> list(String keyword);
    OrderTemplate get(Long id);
    OrderTemplate create(OrderTemplateDTO dto);
    OrderTemplate update(OrderTemplateDTO dto);
    void delete(Long id);
    OrderTemplate useTemplate(Long id);
}
