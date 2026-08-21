package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.SystemValueUpdateDTO;
import com.rxas400adm.as400.model.SysvalRow;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 系统值管理（借鉴旧项目 sysvalManagement）：QSYS2.SYSTEM_VALUE_INFO 只读列表 + 修改（CHGSYSVAL）。
 * 修改为写操作，按钮级授权 SYSVAL_EDIT；数据源按 X-AS400-Server 头路由当前服务器。
 */
@RestController
@RequestMapping("/api/v1/system-values")
@RequiredArgsConstructor
@Tag(name = "系统值")
public class SystemValueController {

    private final AS400ClientProvider clientProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSVAL_VIEW')")
    public ApiResponse<List<SysvalRow>> list(@RequestParam(required = false) String keyword) {
        List<SysvalRow> rows = clientProvider.current().listSystemValues();
        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.success(rows);
        }
        String kw = keyword.trim().toUpperCase();
        return ApiResponse.success(rows.stream()
                .filter(r -> r.name() != null && r.name().toUpperCase().contains(kw)
                        || r.description() != null && r.description().toUpperCase().contains(kw))
                .toList());
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('SYSVAL_VIEW')")
    public ApiResponse<SysvalRow> detail(@RequestParam String name) {
        AS400Client client = clientProvider.current();
        return ApiResponse.success(client.listSystemValues().stream()
                .filter(r -> name.equalsIgnoreCase(String.valueOf(r.name())))
                .findFirst()
                .orElse(null));
    }

    /** 修改系统值（CHGSYSVAL），按钮级授权 SYSVAL_EDIT */
    @PutMapping("/{name}")
    @PreAuthorize("hasAuthority('SYSVAL_EDIT')")
    @OperateLog(module = "系统值", operation = "修改系统值")
    public ApiResponse<CommandResult> update(@PathVariable String name, @Valid @RequestBody SystemValueUpdateDTO dto) {
        return ApiResponse.success(clientProvider.current().changeSystemValue(name, dto.getValue()));
    }
}
