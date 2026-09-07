package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.dto.CycleCountPlanDTO;
import com.rxas400adm.as400.dto.CycleCountResultDTO;
import com.rxas400adm.as400.entity.CycleCountPlan;
import com.rxas400adm.as400.entity.CycleCountResult;
import com.rxas400adm.as400.mapper.CycleCountPlanMapper;
import com.rxas400adm.as400.mapper.CycleCountResultMapper;
import com.rxas400adm.as400.vo.CycleCountPlanVO;
import com.rxas400adm.as400.vo.CycleCountResultVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 循环盘点服务实现（㉙）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleCountServiceImpl implements ICycleCountService {

    private final CycleCountPlanMapper planMapper;
    private final CycleCountResultMapper resultMapper;

    @Override
    public CycleCountPlanVO createPlan(CycleCountPlanDTO dto, String operator) {
        CycleCountPlan plan = new CycleCountPlan();
        plan.setPlanNo("CC" + System.currentTimeMillis() % 1000000);
        plan.setItem(dto.getItem().toUpperCase());
        plan.setItemDesc(dto.getItemDesc());
        plan.setWarehouse(dto.getWarehouse().toUpperCase());
        plan.setPlannedDate(dto.getPlannedDate());
        plan.setStatus("PENDING");
        plan.setAbcClass(dto.getAbcClass());
        plan.setCreatedBy(operator);
        planMapper.insert(plan);
        log.info("创建盘点计划: {}, 物料: {}, 仓库: {}", plan.getPlanNo(), plan.getItem(), plan.getWarehouse());
        return toPlanVO(plan);
    }

    @Override
    public List<CycleCountPlanVO> listPlans(String status, int limit) {
        LambdaQueryWrapper<CycleCountPlan> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(CycleCountPlan::getStatus, status);
        }
        wrapper.orderByDesc(CycleCountPlan::getCreatedTime);
        wrapper.last(PageConstants.limitClause(limit));
        return planMapper.selectList(wrapper).stream().map(this::toPlanVO).collect(Collectors.toList());
    }

    @Override
    public CycleCountResultVO recordResult(CycleCountResultDTO dto, String operator, int systemQty) {
        CycleCountPlan plan = planMapper.selectById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Cycle count plan not found: " + dto.getPlanId());
        }
        if (!"PENDING".equals(plan.getStatus()) && !"IN_PROGRESS".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan status does not allow recording results: " + plan.getStatus());
        }

        // 更新计划状态
        if ("PENDING".equals(plan.getStatus())) {
            plan.setStatus("IN_PROGRESS");
            plan.setOperator(operator);
            planMapper.updateById(plan);
        }

        // 创建结果
        CycleCountResult result = new CycleCountResult();
        result.setPlanId(plan.getId());
        result.setPlanNo(plan.getPlanNo());
        result.setItem(plan.getItem());
        result.setWarehouse(plan.getWarehouse());
        result.setSystemQty(systemQty);
        result.setCountedQty(dto.getCountedQty());
        result.setDifference(dto.getCountedQty() - systemQty);
        result.setDifferenceValue(BigDecimal.ZERO); // 需要从 IIM 获取单价计算
        result.setReason(dto.getReason());
        result.setCountedBy(operator);
        result.setCountTime(LocalDateTime.now());
        resultMapper.insert(result);

        log.info("录入盘点结果: 计划={}, 物料={}, 系统={}, 实盘={}, 差异={}",
                plan.getPlanNo(), plan.getItem(), systemQty, dto.getCountedQty(), result.getDifference());
        return toResultVO(result);
    }

    @Override
    public List<CycleCountResultVO> listResults(Long planId) {
        LambdaQueryWrapper<CycleCountResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CycleCountResult::getPlanId, planId);
        wrapper.orderByAsc(CycleCountResult::getItem);
        return resultMapper.selectList(wrapper).stream().map(this::toResultVO).collect(Collectors.toList());
    }

    @Override
    public Object getSummary(String fromDate, String toDate) {
        // 简化实现：返回基本统计
        LambdaQueryWrapper<CycleCountResult> wrapper = new LambdaQueryWrapper<>();
        List<CycleCountResult> results = resultMapper.selectList(wrapper);
        int totalItems = results.size();
        int matchedItems = (int) results.stream().filter(r -> r.getDifference() == 0).count();
        int mismatchedItems = totalItems - matchedItems;
        return Map.of(
                "totalItems", totalItems,
                "matchedItems", matchedItems,
                "mismatchedItems", mismatchedItems,
                "accuracyRate", totalItems > 0 ? BigDecimal.valueOf(matchedItems).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalItems), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO
        );
    }

    private CycleCountPlanVO toPlanVO(CycleCountPlan p) {
        return new CycleCountPlanVO(
                p.getId(), p.getPlanNo(), p.getItem(), p.getItemDesc(),
                p.getWarehouse(), p.getPlannedDate() != null ? p.getPlannedDate().toString() : null,
                p.getStatus(), p.getAbcClass(), p.getOperator(), p.getCreatedBy(),
                p.getCreatedTime() != null ? p.getCreatedTime().toString() : null
        );
    }

    private CycleCountResultVO toResultVO(CycleCountResult r) {
        return new CycleCountResultVO(
                r.getId(), r.getPlanNo(), r.getItem(), r.getWarehouse(),
                r.getSystemQty(), r.getCountedQty(), r.getDifference(),
                r.getDifferenceValue(), r.getReason(), r.getCountedBy(),
                r.getCountTime() != null ? r.getCountTime().toString() : null
        );
    }
}
