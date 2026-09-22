package com.rxas400adm.cost.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.cost.entity.CostVariance;
import com.rxas400adm.cost.mapper.CostVarianceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostVarianceService {

    private final CostVarianceMapper mapper;

    public PageResult<CostVariance> pageQuery(String cono, String itemCode, String costComponent,
                                              String varianceType, String period, String status,
                                              int current, int size) {
        LambdaQueryWrapper<CostVariance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(CostVariance::getCono, cono);
        if (StringUtils.hasText(itemCode)) wrapper.eq(CostVariance::getItemCode, itemCode);
        if (StringUtils.hasText(costComponent)) wrapper.eq(CostVariance::getCostComponent, costComponent);
        if (StringUtils.hasText(varianceType)) wrapper.eq(CostVariance::getVarianceType, varianceType);
        if (StringUtils.hasText(period)) wrapper.eq(CostVariance::getPeriod, period);
        if (StringUtils.hasText(status)) wrapper.eq(CostVariance::getStatus, status);
        wrapper.orderByDesc(CostVariance::getCreatedTime);

        Page<CostVariance> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public CostVariance getById(Long id) {
        return EntityUtil.require(id, "CostVariance", mapper::selectById);
    }
}
