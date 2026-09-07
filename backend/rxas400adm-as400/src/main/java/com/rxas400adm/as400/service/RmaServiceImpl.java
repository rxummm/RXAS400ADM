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
import java.util.Set;
import java.util.UUID;

/**
 * 退货 RMA 服务实现。
 */
@Service
@RequiredArgsConstructor
public class RmaServiceImpl implements IRmaService {

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "APPROVED", "PROCESSING", "COMPLETED", "CANCELLED");
    private static final java.util.Map<String, Set<String>> ALLOWED_TRANSITIONS = java.util.Map.of(
            "PENDING", Set.of("APPROVED", "CANCELLED"),
            "APPROVED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("COMPLETED", "CANCELLED"),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of()
    );

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
            throw new BusinessException(ErrorCode.NOT_FOUND, "RMA not found: " + id);
        }
        return r;
    }

    @Override
    public Rma create(RmaDTO dto, String operator) {
        Rma r = new Rma();
        r.setRmaNo("RMA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
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
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid RMA status: " + status);
        }
        Rma r = get(id);
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(r.getStatus(), Set.of());
        if (!allowed.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Invalid status transition: " + r.getStatus() + " -> " + status);
        }
        r.setStatus(status);
        r.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mapper.updateById(r);
        return r;
    }
}
