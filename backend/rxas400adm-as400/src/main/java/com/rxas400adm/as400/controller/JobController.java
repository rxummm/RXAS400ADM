package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.service.IJobService;
import com.rxas400adm.as400.vo.JobInfo;
import com.rxas400adm.as400.vo.JobLogVO;
import com.rxas400adm.as400.vo.MsgwMessageVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Job 中心：活动作业（WRKACTJOB）查询、MSGW/LCKW 过滤、作业控制（ENDJOB/HLDJOB/RLSJOB）、
 * 作业日志（DSPJOBLOG）、MSGW 消息查看/应答（WRKMSG/RPLMSG）。
 * 数据源按 X-AS400-Server 头路由到对应服务器。
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "作业管理")
public class JobController {

    private final IJobService jobService;

    @GetMapping
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobInfo>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(jobService.activeJobs(status));
    }

    @GetMapping("/msgw")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobInfo>> msgw() {
        return ApiResponse.success(jobService.msgwJobs());
    }

    @GetMapping("/lckw")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobInfo>> lckw() {
        return ApiResponse.success(jobService.lckwJobs());
    }

    /** P1: 改为 @RequestParam 避免 IBM i 作业名含 / 导致路由解析错误 */
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<JobInfo> detail(@RequestParam String jobName,
                                       @RequestParam String jobUser,
                                       @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.jobDetail(jobName, jobUser, jobNumber));
    }

    @PostMapping("/end")
    @PreAuthorize("hasAuthority('JOB_END')")
    @OperateLog(module = "Job 中心", operation = "ENDJOB 结束作业")
    public ApiResponse<CommandResult> end(@RequestParam String jobName,
                                          @RequestParam String jobUser,
                                          @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.endJob(jobName, jobUser, jobNumber));
    }

    @PostMapping("/hold")
    @PreAuthorize("hasAuthority('JOB_END')")
    @OperateLog(module = "Job 中心", operation = "HLDJOB 挂起作业")
    public ApiResponse<CommandResult> hold(@RequestParam String jobName,
                                           @RequestParam String jobUser,
                                           @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.holdJob(jobName, jobUser, jobNumber));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAuthority('JOB_END')")
    @OperateLog(module = "Job 中心", operation = "RLSJOB 释放作业")
    public ApiResponse<CommandResult> release(@RequestParam String jobName,
                                              @RequestParam String jobUser,
                                              @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.releaseJob(jobName, jobUser, jobNumber));
    }

    @GetMapping("/log")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobLogVO>> log(@RequestParam String jobName,
                                            @RequestParam String jobUser,
                                            @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.jobLog(jobName, jobUser, jobNumber).stream()
                .map(JobLogVO::from).toList());
    }

    @GetMapping("/queues")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<JobQueueRow>> queues() {
        return ApiResponse.success(jobService.jobQueues());
    }

    @GetMapping("/spool")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<SpoolRow>> spool(@RequestParam(required = false) String jobName,
                                             @RequestParam(required = false) String jobUser,
                                             @RequestParam(required = false) String jobNumber) {
        return ApiResponse.success(jobService.spoolFiles(jobName, jobUser, jobNumber));
    }

    @GetMapping("/msgw/messages")
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<List<MsgwMessageVO>> msgwMessages() {
        return ApiResponse.success(jobService.msgwMessages().stream()
                .map(MsgwMessageVO::from).toList());
    }

    @PostMapping("/reply")
    @PreAuthorize("hasAuthority('JOB_END')")
    @OperateLog(module = "Job 中心", operation = "应答 MSGW 消息")
    public ApiResponse<CommandResult> reply(@RequestParam String jobName,
                                            @RequestParam String jobUser,
                                            @RequestParam String jobNumber) {
        return ApiResponse.success(jobService.replyMsg(jobName, jobUser, jobNumber));
    }
}