package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.dto.OrderScheduleDTO;
import com.rxas400adm.as400.entity.OrderSchedule;
import com.rxas400adm.as400.mapper.OrderScheduleMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单排程服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrderScheduleServiceImpl implements IOrderScheduleService {

    private final OrderScheduleMapper mapper;

    @Override
    public List<OrderSchedule> list(String startDate, String endDate) {
        LambdaQueryWrapper<OrderSchedule> qw = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            qw.ge(OrderSchedule::getStartDate, startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            qw.le(OrderSchedule::getEndDate, endDate);
        }
        qw.orderByAsc(OrderSchedule::getStartDate);
        return mapper.selectList(qw);
    }

    @Override
    public OrderSchedule get(Long id) {
        OrderSchedule s = mapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Schedule not found: " + id);
        }
        return s;
    }

    @Override
    public OrderSchedule create(OrderScheduleDTO dto, String operator) {
        OrderSchedule s = new OrderSchedule();
        s.setCono(dto.getCono() != null ? dto.getCono() : "001");
        s.setOrno(dto.getOrno());
        s.setCust(dto.getCust());
        s.setStartDate(dto.getStartDate());
        s.setEndDate(dto.getEndDate());
        s.setProgress(dto.getProgress() != null ? dto.getProgress() : 0);
        s.setPriority(dto.getPriority() != null ? dto.getPriority() : 5);
        s.setCreatedBy(operator);
        s.setCreatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.insert(s);
        return s;
    }

    @Override
    public OrderSchedule update(OrderScheduleDTO dto) {
        if (dto.getOrno() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Order number is required");
        }
        LambdaQueryWrapper<OrderSchedule> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderSchedule::getCono, dto.getCono() != null ? dto.getCono() : "001");
        qw.eq(OrderSchedule::getOrno, dto.getOrno());
        OrderSchedule s = mapper.selectOne(qw);
        if (s == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Schedule not found: " + dto.getOrno());
        }
        if (dto.getStartDate() != null) s.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) s.setEndDate(dto.getEndDate());
        if (dto.getProgress() != null) s.setProgress(dto.getProgress());
        if (dto.getPriority() != null) s.setPriority(dto.getPriority());
        s.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.updateById(s);
        return s;
    }

    @Override
    public void delete(Long id) {
        EntityUtil.require(id, "Order Schedule", mapper::selectById);
        mapper.deleteById(id);
    }
}
