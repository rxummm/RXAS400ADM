package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.service.JobDependencyService;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 作业依赖图（借鉴旧项目 jobDependencyGraph）：{ nodes, links } 图数据。
 * mock 模式返回仿真图；JT400 基于作业日志 CPF1124（提交关系）构建真实依赖图。
 */
@RestController
@RequestMapping("/api/v1/job-dependency")
@RequiredArgsConstructor
@Tag(name = "作业依赖")
public class JobDependencyController {

    private final JobDependencyService jobDependencyService;

    @GetMapping
    @PreAuthorize("hasAuthority('JOB_VIEW')")
    public ApiResponse<GraphData> graph() {
        return ApiResponse.success(jobDependencyService.graph());
    }
}
