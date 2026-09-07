package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.dto.OrderTemplateDTO;
import com.rxas400adm.as400.entity.OrderTemplate;
import com.rxas400adm.as400.mapper.OrderTemplateMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单模板服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrderTemplateServiceImpl implements IOrderTemplateService {

    private final OrderTemplateMapper mapper;

    @Override
    public List<OrderTemplate> list(String keyword) {
        LambdaQueryWrapper<OrderTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderTemplate::getActive, "Y");
        if (keyword != null && !keyword.isBlank()) {
            qw.like(OrderTemplate::getTemplateName, keyword);
        }
        qw.orderByDesc(OrderTemplate::getId);
        return mapper.selectList(qw);
    }

    @Override
    public OrderTemplate get(Long id) {
        OrderTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Template not found: " + id);
        }
        return t;
    }

    @Override
    public OrderTemplate create(OrderTemplateDTO dto) {
        OrderTemplate t = new OrderTemplate();
        t.setTemplateName(dto.getTemplateName());
        t.setCono(dto.getCono() != null ? dto.getCono() : "001");
        t.setCust(dto.getCust());
        t.setShipTo(dto.getShipTo());
        t.setRemark(dto.getRemark());
        t.setLineJson(dto.getLineJson());
        t.setUseCount(0);
        t.setActive("Y");
        t.setCreatedBy("system");
        mapper.insert(t);
        return t;
    }

    @Override
    public OrderTemplate update(OrderTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Template ID is required");
        }
        OrderTemplate t = get(dto.getId());
        if (dto.getTemplateName() != null) t.setTemplateName(dto.getTemplateName());
        if (dto.getCust() != null) t.setCust(dto.getCust());
        if (dto.getShipTo() != null) t.setShipTo(dto.getShipTo());
        if (dto.getRemark() != null) t.setRemark(dto.getRemark());
        if (dto.getLineJson() != null) t.setLineJson(dto.getLineJson());
        mapper.updateById(t);
        return t;
    }

    @Override
    public void delete(Long id) {
        OrderTemplate t = get(id);
        t.setActive("N");
        mapper.updateById(t);
    }

    @Override
    public OrderTemplate useTemplate(Long id) {
        OrderTemplate t = get(id);
        t.setUseCount(t.getUseCount() + 1);
        mapper.updateById(t);
        return t;
    }
}
