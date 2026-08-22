package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.JobScheduleRequest;
import com.rxas400adm.as400.service.IJobScheduleService;
import com.rxas400adm.as400.vo.JobScheduleHistoryVO;
import com.rxas400adm.as400.vo.JobScheduleVO;
import com.rxas400adm.as400.vo.ScheduleExecuteResultVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 作业调度中心（2.4.4）：定时执行 CL 命令 / SQL 的任务 CRUD、立即执行、历史。
 */
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "作业调度")
public class ScheduleController {

    private final IJobScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ApiResponse<List<JobScheduleVO>> list() {
        return ApiResponse.success(scheduleService.list().stream().map(JobScheduleVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @OperateLog(module = "作业调度", operation = "创建调度任务")
    public ApiResponse<JobScheduleVO> create(@Valid @RequestBody JobScheduleRequest request) {
        return ApiResponse.success(JobScheduleVO.from(scheduleService.create(request, currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @OperateLog(module = "作业调度", operation = "更新调度任务")
    public ApiResponse<JobScheduleVO> update(@PathVariable Long id,
                                             @Valid @RequestBody JobScheduleRequest request) {
        return ApiResponse.success(JobScheduleVO.from(scheduleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @OperateLog(module = "作业调度", operation = "删除调度任务")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @OperateLog(module = "作业调度", operation = "启停调度任务")
    public ApiResponse<JobScheduleVO> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ApiResponse.success(JobScheduleVO.from(scheduleService.toggle(id, enabled)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    @OperateLog(module = "作业调度", operation = "立即执行调度任务")
    public ApiResponse<ScheduleExecuteResultVO> executeNow(@PathVariable Long id) {
        return ApiResponse.success(scheduleService.executeNow(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ApiResponse<List<JobScheduleHistoryVO>> history(@PathVariable Long id) {
        return ApiResponse.success(scheduleService.history(id).stream().map(JobScheduleHistoryVO::from).toList());
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}
