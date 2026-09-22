package com.rxas400adm.mrp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.mrp.entity.MrpRecommendation;
import com.rxas400adm.mrp.mapper.MrpRecommendationMapper;
import com.rxas400adm.mrp.vo.MrpRecommendationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrpRecommendationService {

    private final MrpRecommendationMapper mapper;

    public PageResult<MrpRecommendationVO> pageQuery(String cono, String itemCode, String recommendType,
                                                     String status, int current, int size) {
        LambdaQueryWrapper<MrpRecommendation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(MrpRecommendation::getCono, cono);
        if (StringUtils.hasText(itemCode)) wrapper.eq(MrpRecommendation::getItemCode, itemCode);
        if (StringUtils.hasText(recommendType)) wrapper.eq(MrpRecommendation::getRecommendType, recommendType);
        if (StringUtils.hasText(status)) wrapper.eq(MrpRecommendation::getStatus, status);
        wrapper.orderByDesc(MrpRecommendation::getSuggestedDate);

        Page<MrpRecommendation> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(MrpRecommendationVO::from).toList());
    }

    public void release(Long id) {
        MrpRecommendation rec = EntityUtil.require(id, "MrpRecommendation", mapper::selectById);
        rec.setStatus("RELEASED");
        mapper.updateById(rec);
    }
}
