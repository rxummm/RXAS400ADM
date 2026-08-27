package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.monitor.alert.AlertRule;
import com.rxas400adm.monitor.dto.AlertRuleDTO;
import com.rxas400adm.monitor.mapper.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 告警规则服务（R1 分层清零）：列表 / 创建 / 更新 / 删除 / 启停 / 存在校验。
 * 写接口入参为 Create/Update DTO，禁止 Entity 直传（防伪造 id 等内部字段）。
 */
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleMapper ruleMapper;

    public List<AlertRule> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .orderByAsc(AlertRule::getMetricName)
                .orderByAsc(AlertRule::getThreshold));
    }

    public AlertRule create(AlertRuleDTO dto) {
        AlertRule rule = dto.toEntity();
        rule.setId(null);
        if (rule.getChannel() == null || rule.getChannel().isBlank()) {
            rule.setChannel("ALL");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        ruleMapper.insert(rule);
        return rule;
    }

    public AlertRule update(Long id, AlertRuleDTO dto) {
        EntityUtil.require(id, "告警规则", ruleMapper::selectById);
        AlertRule rule = dto.toEntity();
        rule.setId(id);
        if (rule.getChannel() == null || rule.getChannel().isBlank()) {
            rule.setChannel("ALL");
        }
        ruleMapper.updateById(rule);
        return rule;
    }

    public void delete(Long id) {
        EntityUtil.require(id, "告警规则", ruleMapper::selectById);
        ruleMapper.deleteById(id);
    }

    public AlertRule toggle(Long id, Boolean enabled) {
        AlertRule rule = EntityUtil.require(id, "告警规则", ruleMapper::selectById);
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
        return rule;
    }


}
