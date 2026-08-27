package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.DataAreaCreateDTO;
import com.rxas400adm.as400.dto.DataAreaUpdateDTO;
import com.rxas400adm.as400.model.DataAreaRow;
import com.rxas400adm.as400.vo.DataAreaVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据区域管理：QSYS2.DATA_AREA_INFO 查询 + CHGDTAARA/CRTDTAARA/DLTDTAARA 操作。
 * 数据源按 X-AS400-Server 头路由当前服务器。
 */
@RestController
@RequestMapping("/api/v1/data-areas")
@RequiredArgsConstructor
@Tag(name = "数据区域")
public class DataAreaController {

    private final AS400ClientProvider clientProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSVAL_VIEW')")
    public ApiResponse<List<DataAreaVO>> list(@RequestParam(required = false) String library) {
        List<DataAreaRow> rows = clientProvider.current().listDataAreas(library);
        List<DataAreaVO> vos = rows.stream()
                .map(r -> new DataAreaVO(r.library(), r.name(), r.value(), r.type(), r.length()))
                .toList();
        return ApiResponse.success(vos);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('SYSVAL_VIEW')")
    public ApiResponse<DataAreaVO> detail(@RequestParam String library, @RequestParam String name) {
        DataAreaRow row = clientProvider.current().getDataArea(library, name);
        return ApiResponse.success(new DataAreaVO(row.library(), row.name(), row.value(), row.type(), row.length()));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAuthority('SYSVAL_EDIT')")
    @OperateLog(module = "数据区域", operation = "修改数据区域")
    public ApiResponse<CommandResult> update(@PathVariable String name,
                                              @RequestParam String library,
                                              @RequestBody DataAreaUpdateDTO dto) {
        return ApiResponse.success(clientProvider.current().changeDataArea(library, name, dto.getValue()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSVAL_EDIT')")
    @OperateLog(module = "数据区域", operation = "创建数据区域")
    public ApiResponse<CommandResult> create(@RequestBody DataAreaCreateDTO dto) {
        return ApiResponse.success(clientProvider.current().createDataArea(dto.getLibrary(), dto.getName(), dto.getLength(), dto.getValue()));
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAuthority('SYSVAL_EDIT')")
    @OperateLog(module = "数据区域", operation = "删除数据区域")
    public ApiResponse<CommandResult> delete(@PathVariable String name,
                                              @RequestParam String library) {
        return ApiResponse.success(clientProvider.current().deleteDataArea(library, name));
    }
}
