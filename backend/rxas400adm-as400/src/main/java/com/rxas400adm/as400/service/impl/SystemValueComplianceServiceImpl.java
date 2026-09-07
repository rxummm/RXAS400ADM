package com.rxas400adm.as400.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.entity.SystemValueCompliance;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.as400.mapper.SystemValueComplianceMapper;
import com.rxas400adm.as400.service.ISystemValueComplianceService;
import com.rxas400adm.as400.vo.SystemValueComplianceVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A8 系统值合规检查服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemValueComplianceServiceImpl implements ISystemValueComplianceService {

    private final SystemValueComplianceMapper complianceMapper;
    private final IbmiSystemMapper systemMapper;

    @Override
    public List<SystemValueComplianceVO> listByServer(Long serverId) {
        LambdaQueryWrapper<SystemValueCompliance> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(SystemValueCompliance::getServerId, serverId);
        }
        wrapper.orderByDesc(SystemValueCompliance::getLastChecked);
        List<SystemValueCompliance> records = complianceMapper.selectList(wrapper);

        Map<Long, String> serverNames = systemMapper.selectList(null).stream()
                .collect(Collectors.toMap(IbmiSystem::getId, IbmiSystem::getName, (a, b) -> b));

        return records.stream().map(r -> {
            SystemValueComplianceVO vo = new SystemValueComplianceVO();
            vo.setId(r.getId());
            vo.setServerId(r.getServerId());
            vo.setServerName(serverNames.get(r.getServerId()));
            vo.setSystemValue(r.getSystemValue());
            vo.setCurrentValue(r.getCurrentValue());
            vo.setExpectedValue(r.getExpectedValue());
            vo.setComplianceStatus(r.getComplianceStatus());
            vo.setSeverity(r.getSeverity());
            vo.setDescription(r.getDescription());
            vo.setRemediation(r.getRemediation());
            vo.setLastChecked(r.getLastChecked());
            vo.setCreatedTime(r.getCreatedTime());
            vo.setUpdatedTime(r.getUpdatedTime());
            return vo;
        }).toList();
    }

    @Override
    public SystemValueComplianceVO getLatestByServer(Long serverId) {
        LambdaQueryWrapper<SystemValueCompliance> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(SystemValueCompliance::getServerId, serverId);
        }
        wrapper.orderByDesc(SystemValueCompliance::getLastChecked).last(PageConstants.limitClause(1));
        SystemValueCompliance record = complianceMapper.selectOne(wrapper);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "No compliance record found for server: " + serverId);
        }

        IbmiSystem server = systemMapper.selectById(record.getServerId());
        SystemValueComplianceVO vo = new SystemValueComplianceVO();
        vo.setId(record.getId());
        vo.setServerId(record.getServerId());
        vo.setServerName(server != null ? server.getName() : null);
        vo.setSystemValue(record.getSystemValue());
        vo.setCurrentValue(record.getCurrentValue());
        vo.setExpectedValue(record.getExpectedValue());
        vo.setComplianceStatus(record.getComplianceStatus());
        vo.setSeverity(record.getSeverity());
        vo.setDescription(record.getDescription());
        vo.setRemediation(record.getRemediation());
        vo.setLastChecked(record.getLastChecked());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setUpdatedTime(record.getUpdatedTime());
        return vo;
    }
}
