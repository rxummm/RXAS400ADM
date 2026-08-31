package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.dto.RmaDTO;
import com.rxas400adm.as400.entity.Rma;
import com.rxas400adm.as400.mapper.RmaMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 退货 RMA 服务实现。
 */
@Service
@RequiredArgsConstructor
public class RmaServiceImpl implements IRmaService {

    private final RmaMapper mapper;

    @Override
    public List<Rma> list(String status) {
        LambdaQueryWrapper<Rma> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(Rma::getStatus, status);
        }
        qw.orderByDesc(Rma::getId);
        return mapper.selectList(qw);
    }

    @Override
    public Rma get(Long id) {
        Rma r = mapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RMA 不存在: " + id);
        }
        return r;
    }

    @Override
    public Rma create(RmaDTO dto, String operator) {
        Rma r = new Rma();
        r.setRmaNo("RMA-" + System.currentTimeMillis() % 1000000);
        r.setCono(dto.getCono() != null ? dto.getCono() : "001");
        r.setOrno(dto.getOrno());
        r.setCust(dto.getCust());
        r.setItem(dto.getItem());
        r.setQty(dto.getQty());
        r.setReason(dto.getReason());
        r.setStatus("PENDING");
        r.setCreatedBy(operator);
        r.setCreatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.insert(r);
        return r;
    }

    @Override
    public Rma updateStatus(Long id, String status, String operator) {
        Rma r = get(id);
        r.setStatus(status);
        r.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.updateById(r);
        return r;
    }
}
