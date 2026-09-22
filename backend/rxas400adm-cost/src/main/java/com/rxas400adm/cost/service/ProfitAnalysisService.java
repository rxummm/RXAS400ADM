package com.rxas400adm.cost.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.cost.entity.ProfitAnalysis;
import com.rxas400adm.cost.mapper.ProfitAnalysisMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitAnalysisService {

    private final ProfitAnalysisMapper mapper;

    public PageResult<ProfitAnalysis> pageQuery(String cono, String analysisType, String period,
                                                int current, int size) {
        LambdaQueryWrapper<ProfitAnalysis> wrapper = new LambdaQueryWrapper<>();
        if (cono != null) wrapper.eq(ProfitAnalysis::getCono, cono);
        if (analysisType != null) wrapper.eq(ProfitAnalysis::getAnalysisType, analysisType);
        if (period != null) wrapper.eq(ProfitAnalysis::getPeriod, period);
        wrapper.orderByDesc(ProfitAnalysis::getCreatedTime);

        Page<ProfitAnalysis> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
