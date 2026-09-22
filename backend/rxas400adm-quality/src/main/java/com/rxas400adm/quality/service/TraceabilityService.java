package com.rxas400adm.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.entity.TraceabilityChain;
import com.rxas400adm.quality.mapper.TraceabilityChainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceabilityService {

    private final TraceabilityChainMapper mapper;

    public List<TraceabilityChain> traceUpstream(String itemCode, String batchNo) {
        return mapper.selectList(new LambdaQueryWrapper<TraceabilityChain>()
                .eq(TraceabilityChain::getItemCode, itemCode)
                .eq(TraceabilityChain::getBatchNo, batchNo)
                .eq(TraceabilityChain::getTraceType, "UPSTREAM"));
    }

    public List<TraceabilityChain> traceDownstream(String itemCode, String batchNo) {
        return mapper.selectList(new LambdaQueryWrapper<TraceabilityChain>()
                .eq(TraceabilityChain::getItemCode, itemCode)
                .eq(TraceabilityChain::getBatchNo, batchNo)
                .eq(TraceabilityChain::getTraceType, "DOWNSTREAM"));
    }

    /**
     * 分页追溯查询：支持 itemCode / batchNo / traceType 筛选
     */
    public PageResult<TraceabilityChain> tracePaged(int current, int size,
                                                     String itemCode, String batchNo, String traceType) {
        LambdaQueryWrapper<TraceabilityChain> wrapper = new LambdaQueryWrapper<TraceabilityChain>()
                .eq(itemCode != null && !itemCode.isBlank(), TraceabilityChain::getItemCode, itemCode)
                .eq(batchNo != null && !batchNo.isBlank(), TraceabilityChain::getBatchNo, batchNo)
                .eq(traceType != null && !traceType.isBlank(), TraceabilityChain::getTraceType, traceType)
                .orderByDesc(TraceabilityChain::getCreatedTime);

        long total = mapper.selectCount(wrapper);
        List<TraceabilityChain> records = mapper.selectPage(new Page<>(current, size), wrapper).getRecords();
        return new PageResult<>(total, records);
    }

    public List<TraceabilityChain> listByItemCode(String itemCode) {
        return mapper.selectList(new LambdaQueryWrapper<TraceabilityChain>()
                .eq(TraceabilityChain::getItemCode, itemCode)
                .orderByDesc(TraceabilityChain::getCreatedTime));
    }
}
