package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rxas400adm.system.entity.DashboardWidget;
import com.rxas400adm.system.mapper.DashboardWidgetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * 仪表盘 Widget 显隐偏好（rx_dashboard_widget）：按用户保存。
 * 默认全部显示；用户关闭后写 enabled=0。
 */
@Service
@RequiredArgsConstructor
public class DashboardWidgetService implements IDashboardWidgetService {

    private final DashboardWidgetMapper widgetMapper;

    /** 当前用户所有 widget 偏好（未配置的默认显示） */
    public List<DashboardWidget> prefs(String username) {
        return widgetMapper.selectList(new LambdaQueryWrapper<DashboardWidget>()
                .eq(DashboardWidget::getUsername, username));
    }

    /** 查询单个 widget 是否显示（默认显示） */
    public boolean isEnabled(String username, String widgetKey) {
        DashboardWidget widget = widgetMapper.selectOne(new LambdaQueryWrapper<DashboardWidget>()
                .eq(DashboardWidget::getUsername, username)
                .eq(DashboardWidget::getWidgetKey, widgetKey));
        return widget == null || widget.getEnabled() == null || widget.getEnabled() == 1;
    }

    /** 更新单个 widget 显隐（不存在则插入） */
    
    public DashboardWidget update(String username, String widgetKey, boolean enabled) {
        DashboardWidget existing = widgetMapper.selectOne(new LambdaQueryWrapper<DashboardWidget>()
                .eq(DashboardWidget::getUsername, username)
                .eq(DashboardWidget::getWidgetKey, widgetKey));
        if (existing == null) {
            DashboardWidget widget = new DashboardWidget();
            widget.setUsername(username);
            widget.setWidgetKey(widgetKey);
            widget.setEnabled(enabled ? 1 : 0);
            widgetMapper.insert(widget);
            return widget;
        }
        widgetMapper.update(null, new LambdaUpdateWrapper<DashboardWidget>()
                .eq(DashboardWidget::getId, existing.getId())
                .set(DashboardWidget::getEnabled, enabled ? 1 : 0));
        existing.setEnabled(enabled ? 1 : 0);
        return existing;
    }
}