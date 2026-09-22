package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.as400.entity.OrderChange;
import com.rxas400adm.as400.mapper.OrderChangeMapper;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单变更管理服务实现（只读查询）。
 */
@Service
@RequiredArgsConstructor
public class OrderChangeServiceImpl implements IOrderChangeService {

    private final OrderChangeMapper mapper;

    @Override
    public PageResult<OrderChange> listByOrder(String cono, String orno, int current, int size) {
        LambdaQueryWrapper<OrderChange> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderChange::getCono, cono);
        qw.eq(OrderChange::getOrno, orno);
        qw.orderByDesc(OrderChange::getId);

        IPage<OrderChange> page = mapper.selectPage(new Page<>(current, size), qw);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
