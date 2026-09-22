package com.rxas400adm.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.entity.QualityInspection;
import com.rxas400adm.quality.mapper.QualityInspectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityInspectionService {

    private final QualityInspectionMapper mapper;

    public PageResult<QualityInspection> pageQuery(String inspectionType, String result,
                                                   String itemCode, String batchNo,
                                                   LocalDate fromDate, LocalDate toDate,
                                                   int current, int size) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(inspectionType)) {
            wrapper.eq(QualityInspection::getInspectionType, inspectionType);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(QualityInspection::getResult, result);
        }
        if (StringUtils.hasText(itemCode)) {
            wrapper.eq(QualityInspection::getItemCode, itemCode);
        }
        if (StringUtils.hasText(batchNo)) {
            wrapper.eq(QualityInspection::getBatchNo, batchNo);
        }
        if (fromDate != null) {
            wrapper.ge(QualityInspection::getInspectionDate, fromDate);
        }
        if (toDate != null) {
            wrapper.le(QualityInspection::getInspectionDate, toDate);
        }
        wrapper.orderByDesc(QualityInspection::getCreatedTime);

        Page<QualityInspection> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public QualityInspection getByIdOrThrow(Long id) {
        QualityInspection entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Quality inspection not found: " + id);
        }
        return entity;
    }

    public List<QualityInspection> listByItemCode(String itemCode) {
        return mapper.selectList(new LambdaQueryWrapper<QualityInspection>()
                .eq(QualityInspection::getItemCode, itemCode)
                .orderByDesc(QualityInspection::getInspectionDate));
    }

    public PageResult<QualityInspection> pageByItemCode(String itemCode, int current, int size) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<QualityInspection>()
                .eq(QualityInspection::getItemCode, itemCode)
                .orderByDesc(QualityInspection::getInspectionDate);
        Page<QualityInspection> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<QualityInspection> listByBatchNo(String batchNo) {
        return mapper.selectList(new LambdaQueryWrapper<QualityInspection>()
                .eq(QualityInspection::getBatchNo, batchNo)
                .orderByDesc(QualityInspection::getInspectionDate));
    }

    public PageResult<QualityInspection> pageByBatchNo(String batchNo, int current, int size) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<QualityInspection>()
                .eq(QualityInspection::getBatchNo, batchNo)
                .orderByDesc(QualityInspection::getInspectionDate);
        Page<QualityInspection> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
