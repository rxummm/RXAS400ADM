package com.rxas400adm.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.entity.SpcRecord;
import com.rxas400adm.quality.mapper.SpcRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpcService {

    private final SpcRecordMapper mapper;

    public PageResult<SpcRecord> pageQuery(String itemCode, LocalDate fromDate, LocalDate toDate,
                                           int current, int size) {
        LambdaQueryWrapper<SpcRecord> wrapper = new LambdaQueryWrapper<>();
        if (itemCode != null) {
            wrapper.eq(SpcRecord::getItemCode, itemCode);
        }
        if (fromDate != null) {
            wrapper.ge(SpcRecord::getSampleDate, fromDate);
        }
        if (toDate != null) {
            wrapper.le(SpcRecord::getSampleDate, toDate);
        }
        wrapper.orderByDesc(SpcRecord::getSampleDate).orderByDesc(SpcRecord::getSubgroupNo);

        Page<SpcRecord> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<SpcRecord> listByItemCode(String itemCode, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<SpcRecord>()
                .eq(SpcRecord::getItemCode, itemCode)
                .orderByDesc(SpcRecord::getSampleDate)
                .last(PageConstants.limitClause(limit)));
    }

    public List<SpcRecord> listOutOfControl(String itemCode, LocalDate fromDate, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<SpcRecord>()
                .eq(itemCode != null, SpcRecord::getItemCode, itemCode)
                .eq(SpcRecord::getIsOutOfControl, 1)
                .ge(fromDate != null, SpcRecord::getSampleDate, fromDate)
                .orderByDesc(SpcRecord::getSampleDate)
                .last(PageConstants.limitClause(limit)));
    }

    public PageResult<SpcRecord> pageOutOfControl(String itemCode, LocalDate fromDate,
                                                   int current, int size) {
        LambdaQueryWrapper<SpcRecord> wrapper = new LambdaQueryWrapper<SpcRecord>()
                .eq(itemCode != null, SpcRecord::getItemCode, itemCode)
                .eq(SpcRecord::getIsOutOfControl, 1)
                .ge(fromDate != null, SpcRecord::getSampleDate, fromDate)
                .orderByDesc(SpcRecord::getSampleDate);
        Page<SpcRecord> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
