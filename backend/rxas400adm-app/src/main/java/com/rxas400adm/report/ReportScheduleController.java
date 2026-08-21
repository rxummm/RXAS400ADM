package com.rxas400adm.report;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.report.dto.ReportScheduleDTO;
import com.rxas400adm.report.vo.ScheduleExecuteResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 报表定时任务（2.5.18）：CRUD / 启停 / 立即执行 / 历史。
 * 读操作 REPORT_VIEW，写操作 REPORT_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/report-schedules")
@RequiredArgsConstructor
@Tag(name = "定时报表")
public class ReportScheduleController {

    private final IReportScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<List<ReportScheduleVO>> list() {
        return ApiResponse.success(scheduleService.list().stream().map(ReportScheduleVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REPORT_MANAGE')")
    @OperateLog(module = "报表中心", operation = "创建定时报表任务")
    public ApiResponse<ReportScheduleVO> create(@Valid @RequestBody ReportScheduleDTO schedule) {
        return ApiResponse.success(ReportScheduleVO.from(scheduleService.create(schedule, currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORT_MANAGE')")
    @OperateLog(module = "报表中心", operation = "更新定时报表任务")
    public ApiResponse<ReportScheduleVO> update(@PathVariable Long id, @Valid @RequestBody ReportScheduleDTO schedule) {
        return ApiResponse.success(ReportScheduleVO.from(scheduleService.update(id, schedule)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORT_MANAGE')")
    @OperateLog(module = "报表中心", operation = "删除定时报表任务")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('REPORT_MANAGE')")
    @OperateLog(module = "报表中心", operation = "启停定时报表任务")
    public ApiResponse<ReportScheduleVO> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ApiResponse.success(ReportScheduleVO.from(scheduleService.toggle(id, enabled)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('REPORT_MANAGE')")
    @OperateLog(module = "报表中心", operation = "立即执行定时报表任务")
    public ApiResponse<ScheduleExecuteResultVO> executeNow(@PathVariable Long id) {
        return ApiResponse.success(scheduleService.executeNow(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<List<ReportScheduleHistory>> history(@PathVariable Long id) {
        return ApiResponse.success(scheduleService.history(id));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}