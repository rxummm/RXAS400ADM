package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.QueryRequest;
import com.rxas400adm.as400.service.ISqlQueryService;
import com.rxas400adm.as400.vo.QueryResult;
import com.rxas400adm.as400.vo.SqlHistoryVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * QSYS2 数据查询工具：只读 SELECT 执行 + 查询历史。
 * 数据源按 X-AS400-Server 头路由。
 */
@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@Tag(name = "SQL查询")
public class SqlQueryController {

    private final ISqlQueryService queryService;

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('QUERY_EXECUTE')")
    @OperateLog(module = "数据查询", operation = "执行 SQL 查询")
    public ApiResponse<QueryResult> execute(@Valid @RequestBody QueryRequest request) {
        return ApiResponse.success(queryService.execute(request.getSql()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('QUERY_EXECUTE')")
    public ApiResponse<List<SqlHistoryVO>> history(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(queryService.history(limit).stream().map(SqlHistoryVO::from).toList());
    }
}
