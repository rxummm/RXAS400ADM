package com.rxas400adm.as400.freight;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.util.EntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreightCostService {

    private final FreightCostRuleMapper ruleMapper;
    private final FreightCostMapper recordMapper;

    // ==================== 规则管理 ====================

    public List<FreightCostRule> listRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<FreightCostRule>()
                .orderByAsc(FreightCostRule::getCarrier)
                .orderByAsc(FreightCostRule::getRuleName));
    }

    public FreightCostRule createRule(FreightCostRuleDTO dto) {
        FreightCostRule rule = dto.toEntity();
        rule.setId(null);
        ruleMapper.insert(rule);
        return rule;
    }

    public FreightCostRule updateRule(Long id, FreightCostRuleDTO dto) {
        EntityUtil.require(id, "运费规则", ruleMapper::selectById);
        FreightCostRule rule = dto.toEntity();
        rule.setId(id);
        ruleMapper.updateById(rule);
        return rule;
    }

    public void deleteRule(Long id) {
        EntityUtil.require(id, "运费规则", ruleMapper::selectById);
        ruleMapper.deleteById(id);
    }

    public FreightCostRule toggleRule(Long id, Boolean enabled) {
        FreightCostRule rule = EntityUtil.require(id, "运费规则", ruleMapper::selectById);
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
        return rule;
    }

    // ==================== 运费计算 ====================

    public BigDecimal calculateFreight(String carrier, String costType, BigDecimal quantity) {
        LambdaQueryWrapper<FreightCostRule> qw = new LambdaQueryWrapper<FreightCostRule>()
                .eq(FreightCostRule::getCarrier, carrier)
                .eq(FreightCostRule::getCostType, costType)
                .eq(FreightCostRule::getEnabled, true);
        FreightCostRule rule = ruleMapper.selectOne(qw);
        if (rule == null) return BigDecimal.ZERO;
        return rule.calculate(quantity);
    }

    // ==================== 运费记录 ====================

    public Page<FreightCostRecord> listRecords(FreightCostQueryDTO query) {
        Page<FreightCostRecord> page = new Page<>(query.getAdjustedCurrent(), query.getAdjustedSize());
        LambdaQueryWrapper<FreightCostRecord> qw = new LambdaQueryWrapper<FreightCostRecord>()
                .like(query.orderNo() != null && !query.orderNo().isBlank(),
                        FreightCostRecord::getOrderNo, query.orderNo())
                .like(query.carrier() != null && !query.carrier().isBlank(),
                        FreightCostRecord::getCarrier, query.carrier())
                .orderByDesc(FreightCostRecord::getShipDate);
        return recordMapper.selectPage(page, qw);
    }

    public FreightCostRecord createRecord(FreightCostRecordDTO dto) {
        FreightCostRecord record = new FreightCostRecord();
        record.setOrderNo(dto.orderNo());
        record.setCarrier(dto.carrier());
        record.setWeight(dto.weight());
        record.setVolume(dto.volume());
        record.setPieceCount(dto.pieceCount());
        record.setEstimatedCost(dto.estimatedCost());
        record.setActualCost(dto.actualCost());
        record.setCostDiff(dto.actualCost() != null && dto.estimatedCost() != null
                ? dto.actualCost().subtract(dto.estimatedCost()) : BigDecimal.ZERO);
        record.setShipDate(dto.shipDate());
        recordMapper.insert(record);
        return record;
    }

    public void deleteRecord(Long id) {
        EntityUtil.require(id, "运费记录", recordMapper::selectById);
        recordMapper.deleteById(id);
    }

    // ==================== 成本分析 ====================

    public List<FreightCostTrendVO> getMonthlyTrend(String carrier, int months) {
        return recordMapper.selectList(new LambdaQueryWrapper<FreightCostRecord>()
                .like(carrier != null && !carrier.isBlank(), FreightCostRecord::getCarrier, carrier)
                .isNotNull(FreightCostRecord::getShipDate)
                .orderByDesc(FreightCostRecord::getShipDate)
                .last("LIMIT " + Math.max(1, months * 30)))
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getShipDate() != null ? r.getShipDate().toString().substring(0, 7) : "unknown",
                        Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    List<FreightCostRecord> records = e.getValue();
                    BigDecimal total = records.stream()
                            .map(r -> r.getActualCost() != null ? r.getActualCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = records.isEmpty() ? BigDecimal.ZERO
                            : total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
                    String carrierName = records.get(0).getCarrier();
                    return new FreightCostTrendVO(e.getKey(), total, avg, records.size(), carrierName);
                })
                .sorted(Comparator.comparing(FreightCostTrendVO::month).reversed())
                .toList();
    }

    public Map<String, BigDecimal> getCarrierCostShare() {
        List<FreightCostRecord> all = recordMapper.selectList(new LambdaQueryWrapper<FreightCostRecord>()
                .isNotNull(FreightCostRecord::getActualCost)
                .orderByDesc(FreightCostRecord::getShipDate)
                .last("LIMIT 1000"));
        return all.stream()
                .collect(Collectors.groupingBy(
                        FreightCostRecord::getCarrier,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                r -> r.getActualCost() != null ? r.getActualCost() : BigDecimal.ZERO,
                                BigDecimal::add)));
    }
}
