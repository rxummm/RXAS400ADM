package com.rxas400adm.cost.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.cost.entity.StandardCost;
import com.rxas400adm.cost.mapper.StandardCostMapper;
import com.rxas400adm.cost.vo.StandardCostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardCostService {

    private final StandardCostMapper mapper;

    public PageResult<StandardCostVO> pageQuery(String itemCode, String costComponent,
                                                 String status, LocalDate fromDate, LocalDate toDate,
                                                 int current, int size) {
        LambdaQueryWrapper<StandardCost> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(itemCode)) {
            wrapper.eq(StandardCost::getItemCode, itemCode);
        }
        if (StringUtils.hasText(costComponent)) {
            wrapper.eq(StandardCost::getCostComponent, costComponent);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(StandardCost::getStatus, status);
        }
        if (fromDate != null) {
            wrapper.ge(StandardCost::getEffectiveDate, fromDate);
        }
        if (toDate != null) {
            wrapper.le(StandardCost::getEffectiveDate, toDate);
        }
        wrapper.orderByDesc(StandardCost::getCreatedTime);

        Page<StandardCost> page = mapper.selectPage(new Page<>(current, size), wrapper);
        List<StandardCostVO> voList = page.getRecords().stream()
                .map(StandardCostVO::from)
                .toList();
        return new PageResult<>(page.getTotal(), voList);
    }

    public StandardCostVO getById(Long id) {
        StandardCost entity = EntityUtil.require(id, "StandardCost", mapper::selectById);
        return StandardCostVO.from(entity);
    }

    public List<StandardCostVO> listByItemCode(String itemCode) {
        return mapper.selectList(new LambdaQueryWrapper<StandardCost>()
                .eq(StandardCost::getItemCode, itemCode)
                .eq(StandardCost::getStatus, "ACTIVE")
                .orderByDesc(StandardCost::getEffectiveDate))
                .stream()
                .map(StandardCostVO::from)
                .toList();
    }
}
