package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.DashboardWidget;

/**
 * 仪表盘 Widget 视图：与 DashboardWidget 字段契约解耦。
 */
public record DashboardWidgetVO(
        Long id,
        String username,
        String widgetKey,
        Integer enabled) {

    public static DashboardWidgetVO from(DashboardWidget e) {
        return new DashboardWidgetVO(
                e.getId(), e.getUsername(), e.getWidgetKey(), e.getEnabled());
    }
}