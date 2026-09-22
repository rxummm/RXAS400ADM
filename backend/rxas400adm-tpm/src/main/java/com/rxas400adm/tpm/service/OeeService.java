package com.rxas400adm.tpm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.tpm.entity.OeeRecord;
import com.rxas400adm.tpm.mapper.OeeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OeeService {

    private final OeeRecordMapper mapper;

    public PageResult<OeeRecord> pageQuery(Long equipmentId, LocalDate fromDate, LocalDate toDate,
                                           int current, int size) {
        LambdaQueryWrapper<OeeRecord> wrapper = new LambdaQueryWrapper<>();
        if (equipmentId != null) wrapper.eq(OeeRecord::getEquipmentId, equipmentId);
        if (fromDate != null) wrapper.ge(OeeRecord::getCalcDate, fromDate);
        if (toDate != null) wrapper.le(OeeRecord::getCalcDate, toDate);
        wrapper.orderByDesc(OeeRecord::getCalcDate);

        Page<OeeRecord> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public OeeRecord getLatestByEquipment(Long equipmentId) {
        OeeRecord entity = mapper.selectOne(new LambdaQueryWrapper<OeeRecord>()
                .eq(OeeRecord::getEquipmentId, equipmentId)
                .orderByDesc(OeeRecord::getCalcDate)
                .last(PageConstants.limitClause(1)));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "OEE record not found for equipment: " + equipmentId);
        }
        return entity;
    }
}
