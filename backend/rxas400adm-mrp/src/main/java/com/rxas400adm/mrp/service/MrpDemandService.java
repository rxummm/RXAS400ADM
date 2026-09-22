package com.rxas400adm.mrp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.mrp.entity.MrpDemand;
import com.rxas400adm.mrp.mapper.MrpDemandMapper;
import com.rxas400adm.mrp.vo.MrpDemandVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrpDemandService {

    private final MrpDemandMapper mapper;

    public PageResult<MrpDemandVO> pageQuery(String cono, String itemCode, String demandType,
                                             String status, int current, int size) {
        LambdaQueryWrapper<MrpDemand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(MrpDemand::getCono, cono);
        if (StringUtils.hasText(itemCode)) wrapper.eq(MrpDemand::getItemCode, itemCode);
        if (StringUtils.hasText(demandType)) wrapper.eq(MrpDemand::getDemandType, demandType);
        if (StringUtils.hasText(status)) wrapper.eq(MrpDemand::getStatus, status);
        wrapper.orderByDesc(MrpDemand::getRequiredDate);

        Page<MrpDemand> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(MrpDemandVO::from).toList());
    }

    public MrpDemandVO getById(Long id) {
        MrpDemand entity = EntityUtil.require(id, "MrpDemand", mapper::selectById);
        return MrpDemandVO.from(entity);
    }

    public void updateStatus(Long id, String status) {
        MrpDemand demand = EntityUtil.require(id, "MrpDemand", mapper::selectById);
        demand.setStatus(status);
        mapper.updateById(demand);
    }
}
