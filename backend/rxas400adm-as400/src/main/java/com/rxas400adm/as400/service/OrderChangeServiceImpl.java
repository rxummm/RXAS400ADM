package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.OrderChange;
import com.rxas400adm.as400.mapper.OrderChangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单变更管理服务实现（只读查询）。
 */
@Service
@RequiredArgsConstructor
public class OrderChangeServiceImpl implements IOrderChangeService {

    private final OrderChangeMapper mapper;

    @Override
    public List<OrderChange> listByOrder(String cono, String orno) {
        LambdaQueryWrapper<OrderChange> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderChange::getCono, cono);
        qw.eq(OrderChange::getOrno, orno);
        qw.orderByDesc(OrderChange::getId);
        return mapper.selectList(qw);
    }
}
