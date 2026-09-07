package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsWabpConfigDTO;
import com.rxas400adm.as400.dto.BpcsWabpImportResult;
import com.rxas400adm.as400.service.IBpcsWabpService;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.as400.vo.BpcsWabpConfigVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * WABP auto-distribution configuration Controller.
 */
@RestController
@RequestMapping("/api/v1/bpcs/wabp")
@RequiredArgsConstructor
@Tag(name = "BPCS WABP", description = "WABP auto-distribution configuration")
public class BpcsWabpController {

    private final IBpcsWabpService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsWabpConfigVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listConfigs(cono, limit));
    }

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<BpcsWabpConfigVO> get(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String wh,
            @RequestParam int dayOfWeek) {
        return ApiResponse.success(service.getConfig(cono, wh, dayOfWeek));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Create WABP config")
    public ApiResponse<Void> create(
            @RequestParam(defaultValue = "001") String cono,
            @Valid @RequestBody BpcsWabpConfigDTO dto) {
        service.createConfig(cono, dto);
        return ApiResponse.success(null);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Update WABP config")
    public ApiResponse<Void> update(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String wh,
            @RequestParam int dayOfWeek,
            @Valid @RequestBody BpcsWabpConfigDTO dto) {
        service.updateConfig(cono, wh, dayOfWeek, dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Delete WABP config")
    public ApiResponse<Void> delete(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String wh,
            @RequestParam int dayOfWeek) {
        service.deleteConfig(cono, wh, dayOfWeek);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Import WABP configs")
    public ApiResponse<BpcsWabpImportResult> importExcel(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam("file") MultipartFile file) throws IOException {
        List<BpcsWabpConfigDTO> list = service.parseExcel(file);
        return ApiResponse.success(service.importConfigs(cono, list));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsWabpConfigVO>> export(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(service.exportConfigs(cono));
    }
}
