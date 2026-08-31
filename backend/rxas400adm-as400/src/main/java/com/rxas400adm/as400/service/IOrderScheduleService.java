package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.OrderScheduleDTO;
import com.rxas400adm.as400.entity.OrderSchedule;

import java.util.List;

/**
 * 订单排程服务接口（CRUD）。
 */
public interface IOrderScheduleService {
    List<OrderSchedule> list(String startDate, String endDate);
    OrderSchedule get(Long id);
    OrderSchedule create(OrderScheduleDTO dto, String operator);
    OrderSchedule update(OrderScheduleDTO dto);
    void delete(Long id);
}
