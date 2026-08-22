package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.JobSlaDTO;
import com.rxas400adm.as400.service.IJobSlaService;
import com.rxas400adm.as400.vo.JobSlaVO;
import com.rxas400adm.as400.vo.SlaExecutionVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 作业 SLA（借鉴旧项目 jobSla）：关键作业预期耗时规则 CRUD + 最近执行对比。
 * 规则为平台本地配置（rx_job_sla），执行数据来自 AS400Client.jobSlaExecutions()
 * （Mock 仿真 / JT400 真实作业历史 QSYS2.JOB_LOG_INFO）。
 */
@RestController
@RequestMapping("/api/v1/job-sla")
@RequiredArgsConstructor
@Tag(name = "作业SLA")
public class JobSlaController {

    private final IJobSlaService slaService;

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobSlaVO>> rules() {
        return ApiResponse.success(slaService.list().stream().map(JobSlaVO::from).toList());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('SLA_MANAGE')")
    @OperateLog(module = "作业 SLA", operation = "新增 SLA 规则")
    public ApiResponse<JobSlaVO> create(@Valid @RequestBody JobSlaDTO sla) {
        return ApiResponse.success(JobSlaVO.from(slaService.create(sla, currentUsername())));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('SLA_MANAGE')")
    @OperateLog(module = "作业 SLA", operation = "修改 SLA 规则")
    public ApiResponse<JobSlaVO> update(@PathVariable Long id, @Valid @RequestBody JobSlaDTO sla) {
        return ApiResponse.success(JobSlaVO.from(slaService.update(id, sla)));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('SLA_MANAGE')")
    @OperateLog(module = "作业 SLA", operation = "删除 SLA 规则")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        slaService.delete(id);
        return ApiResponse.success(null);
    }

    /** 最近执行对比（Mock 仿真 / JT400 真实作业历史） */
    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<SlaExecutionVO>> executions() {
        return ApiResponse.success(slaService.executions().stream()
                .map(SlaExecutionVO::from).toList());
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}