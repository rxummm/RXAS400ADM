package com.rxas400adm.monitor.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.monitor.dto.AlertRuleDTO;
import com.rxas400adm.monitor.service.AlertRuleService;
import com.rxas400adm.monitor.vo.AlertRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 告警规则管理：支持按服务器维度（serverId 空=全部）。写操作需 ALERT_MANAGE。
 * R1 分层清零：Mapper/QueryWrapper 已下沉至 AlertRuleService。
 */
@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
@Tag(name = "告警规则")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<List<AlertRuleVO>> list() {
        return ApiResponse.success(alertRuleService.list().stream().map(AlertRuleVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ALERT_MANAGE')")
    @OperateLog(module = "告警规则", operation = "新增告警规则")
    public ApiResponse<AlertRuleVO> create(@Valid @RequestBody AlertRuleDTO dto) {
        return ApiResponse.success(AlertRuleVO.from(alertRuleService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERT_MANAGE')")
    @OperateLog(module = "告警规则", operation = "更新告警规则")
    public ApiResponse<AlertRuleVO> update(@PathVariable Long id, @Valid @RequestBody AlertRuleDTO dto) {
        return ApiResponse.success(AlertRuleVO.from(alertRuleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERT_MANAGE')")
    @OperateLog(module = "告警规则", operation = "删除告警规则")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        alertRuleService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ALERT_MANAGE')")
    @OperateLog(module = "告警规则", operation = "启停告警规则")
    public ApiResponse<AlertRuleVO> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ApiResponse.success(AlertRuleVO.from(alertRuleService.toggle(id, enabled)));
    }
}
