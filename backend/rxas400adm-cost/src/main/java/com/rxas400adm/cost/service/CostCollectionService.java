package com.rxas400adm.cost.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.cost.entity.CostCollection;
import com.rxas400adm.cost.mapper.CostCollectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostCollectionService {

    private final CostCollectionMapper mapper;

    public PageResult<CostCollection> pageQuery(String cono, String costType, String costObjectType,
                                                String period, String status, int current, int size) {
        LambdaQueryWrapper<CostCollection> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(CostCollection::getCono, cono);
        if (StringUtils.hasText(costType)) wrapper.eq(CostCollection::getCostType, costType);
        if (StringUtils.hasText(costObjectType)) wrapper.eq(CostCollection::getCostObjectType, costObjectType);
        if (StringUtils.hasText(period)) wrapper.eq(CostCollection::getPeriod, period);
        if (StringUtils.hasText(status)) wrapper.eq(CostCollection::getStatus, status);
        wrapper.orderByDesc(CostCollection::getCreatedTime);

        Page<CostCollection> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public CostCollection getById(Long id) {
        return EntityUtil.require(id, "CostCollection", mapper::selectById);
    }

    public List<CostCollection> listByPeriod(String period) {
        return mapper.selectList(new LambdaQueryWrapper<CostCollection>()
                .eq(CostCollection::getPeriod, period)
                .orderByDesc(CostCollection::getCreatedTime));
    }
}
