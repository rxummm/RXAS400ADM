package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.service.IDashboardWidgetService;
import com.rxas400adm.system.vo.DashboardWidgetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 仪表盘 Widget 显隐偏好（rx_dashboard_widget）：登录用户自定义首页卡片展示。
 */
@RestController
@RequestMapping("/api/v1/dashboard/widgets")
@RequiredArgsConstructor
@Tag(name = "仪表盘组件")
public class DashboardWidgetController {

    private final IDashboardWidgetService widgetService;

    @GetMapping
    public ApiResponse<List<DashboardWidgetVO>> prefs() {
        return ApiResponse.success(widgetService.prefs(currentUsername()).stream().map(DashboardWidgetVO::from).toList());
    }

    // P3-8：显式声明仅登录可写 + 审计（写操作不允许裸奔到匿名链/无审计）
    @PutMapping("/{widgetKey}")
    @PreAuthorize("isAuthenticated()")
    @OperateLog(module = "仪表盘", operation = "更新 Widget 显隐")
    public ApiResponse<DashboardWidgetVO> update(@PathVariable String widgetKey,
                                               @RequestParam boolean enabled) {
        return ApiResponse.success(DashboardWidgetVO.from(widgetService.update(currentUsername(), widgetKey, enabled)));
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}