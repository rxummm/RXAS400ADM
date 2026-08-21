package com.rxas400adm.system.service;

import com.rxas400adm.system.entity.DashboardWidget;

import java.util.List;

/**
 * 仪表盘 Widget 显隐偏好服务接口（rx_dashboard_widget）。
 */
public interface IDashboardWidgetService {

    List<DashboardWidget> prefs(String username);

    boolean isEnabled(String username, String widgetKey);

    DashboardWidget update(String username, String widgetKey, boolean enabled);
}
