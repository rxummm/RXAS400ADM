package com.rxas400adm.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台健康巡检数据服务（R1 分层清零：Controller 不碰 Mapper/QueryWrapper）。
 * 聚合 DB 探测 / 服务器列表 / 未关闭告警计数，供 HealthController 组装巡检报告。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final IbmiSystemMapper systemMapper;
    private final AlertEventMapper alertEventMapper;

    /** DB 连通性轻量探测 */
    public boolean probeDatabase() {
        try {
            systemMapper.selectCount(null);
            return true;
        } catch (Exception e) {
            log.debug("DB probe failed: {}", e.getMessage());
            return false;
        }
    }

    /** 按 sortOrder 升序的全部服务器（健康巡检逐台连接测试用） */
    public List<IbmiSystem> listSystemsOrdered() {
        return systemMapper.selectList(new LambdaQueryWrapper<IbmiSystem>()
                .orderByAsc(IbmiSystem::getSortOrder));
    }

    /** 未关闭告警数（status=OPEN） */
    public long countOpenAlerts() {
        return alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getStatus, "OPEN"));
    }
}
