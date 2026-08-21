package com.rxas400adm.compile.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.compile.dto.CompileRequest;
import com.rxas400adm.compile.service.ICompileService;
import com.rxas400adm.compile.vo.CompileRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/compile")
@RequiredArgsConstructor
@Tag(name = "编译管理")
public class CompileController {

    private final ICompileService compileService;

    @PostMapping
    @PreAuthorize("hasAuthority('COMPILE_EXECUTE')")
    @OperateLog(module = "编译中心", operation = "编译成员")
    public ApiResponse<CompileRecordVO> compile(@Valid @RequestBody CompileRequest request) {
        return ApiResponse.success(CompileRecordVO.from(compileService.compile(request)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('COMPILE_EXECUTE')")
    public ApiResponse<List<CompileRecordVO>> history() {
        return ApiResponse.success(compileService.history().stream().map(CompileRecordVO::from).toList());
    }
}