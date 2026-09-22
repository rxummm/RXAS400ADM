package com.rxas400adm.tpm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.tpm.entity.MaintenancePlan;
import com.rxas400adm.tpm.mapper.MaintenancePlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenancePlanMapper planMapper;

    public PageResult<MaintenancePlan> pageQuery(String cono, String status,
                                                 LocalDate fromDate, LocalDate toDate,
                                                 int current, int size) {
        LambdaQueryWrapper<MaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(MaintenancePlan::getCono, cono);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenancePlan::getStatus, status);
        if (fromDate != null) wrapper.ge(MaintenancePlan::getNextDueDate, fromDate);
        if (toDate != null) wrapper.le(MaintenancePlan::getNextDueDate, toDate);
        wrapper.orderByAsc(MaintenancePlan::getNextDueDate);

        Page<MaintenancePlan> page = planMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public MaintenancePlan getById(Long id) {
        MaintenancePlan entity = planMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Maintenance plan not found: " + id);
        }
        return entity;
    }
}
