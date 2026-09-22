package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 告警事件服务（R1 分层清零）。
 */
@Service
@RequiredArgsConstructor
public class AlertEventService {

    private final AlertEventMapper alertEventMapper;

    /** 按创建时间倒序取最近 N 条（上限 200，避免全表查出再内存截断） */
    public List<AlertEvent> recent(int limit) {
        return alertEventMapper.selectList(new LambdaQueryWrapper<AlertEvent>()
                .orderByDesc(AlertEvent::getCreatedTime)
                .last(PageConstants.limitClause(Math.min(Math.max(limit, 1), 200))));
    }

    /** 分页查询告警事件（按创建时间倒序） */
    public PageResult<AlertEvent> pageRecent(int current, int size) {
        Page<AlertEvent> page = alertEventMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<AlertEvent>()
                        .orderByDesc(AlertEvent::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
