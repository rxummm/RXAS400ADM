package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.ExecutionService;
import com.rxas400adm.as400.vo.ExecutionRecordVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 执行历史审计页：聚合 作业调度执行历史 + 命令脚本执行记录，
 * 按用户/服务器/时间查看（2.4.4 / 2.4.3 的审计视角）。
 * 支持 来源/状态/关键字 过滤 + 后端分页（P0/P1 大数据页优化）。
 * R1 分层清零：Mapper/QueryWrapper 已下沉至 ExecutionService。
 */
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Tag(name = "执行记录")
public class ExecutionController {

    private final ExecutionService executionService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXECUTION_VIEW')")
    public ApiResponse<PageResult<ExecutionRecordVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "500") int limit) {
        int page = Math.max(1, current);
        int pageSize = Math.max(1, Math.min(size, 200));
        int capped = Math.max(1, Math.min(limit, 500));

        boolean schedulesOnly = "schedule".equalsIgnoreCase(type);
        boolean scriptsOnly = "script".equalsIgnoreCase(type);

        List<Map<String, Object>> merged = new ArrayList<>();
        if (!scriptsOnly) {
            merged.addAll(executionService.scheduleExecutions(status, keyword, capped));
        }
        if (!schedulesOnly) {
            merged.addAll(executionService.scriptExecutions(status, keyword, capped));
        }
        merged.sort((a, b) -> String.valueOf(b.get("runTime")).compareTo(String.valueOf(a.get("runTime"))));

        int total = merged.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<ExecutionRecordVO> records = from >= to ? List.of()
                : merged.subList(from, to).stream().map(ExecutionRecordVO::from).toList();
        return ApiResponse.success(new PageResult<>(total, records));
    }
}
