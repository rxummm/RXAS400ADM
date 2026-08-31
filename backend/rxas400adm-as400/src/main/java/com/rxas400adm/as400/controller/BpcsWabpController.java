package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsWabpConfigDTO;
import com.rxas400adm.as400.dto.BpcsWabpImportResult;
import com.rxas400adm.as400.service.IBpcsWabpService;
import com.rxas400adm.as400.vo.BpcsWabpConfigVO;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * ㊽ WABP 自动分货配置 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/wabp")
@RequiredArgsConstructor
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
    public ApiResponse<Void> create(
            @RequestParam(defaultValue = "001") String cono,
            @Valid @RequestBody BpcsWabpConfigDTO dto) {
        service.createConfig(cono, dto);
        return ApiResponse.success(null);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
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
    public ApiResponse<Void> delete(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String wh,
            @RequestParam int dayOfWeek) {
        service.deleteConfig(cono, wh, dayOfWeek);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    public ApiResponse<BpcsWabpImportResult> importExcel(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        List<BpcsWabpConfigDTO> list = parseExcel(file);
        return ApiResponse.success(service.importConfigs(cono, list));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsWabpConfigVO>> export(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(service.exportConfigs(cono));
    }

    private List<BpcsWabpConfigDTO> parseExcel(MultipartFile file) throws IOException {
        List<BpcsWabpConfigDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 跳过表头
                Row row = sheet.getRow(i);
                if (row == null) continue;
                BpcsWabpConfigDTO dto = new BpcsWabpConfigDTO();
                dto.setWh(getCellString(row, 0));
                dto.setDayOfWeek(getCellInt(row, 1));
                dto.setTime(getCellString(row, 2));
                dto.setShipHold(getCellString(row, 3));
                dto.setCrHold(getCellString(row, 4));
                dto.setPrHold(getCellString(row, 5));
                dto.setActive(getCellString(row, 6));
                dto.setMaintUser("IMPORT");
                list.add(dto);
            }
        }
        return list;
    }

    private String getCellString(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Integer getCellInt(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String val = getCellString(row, cellIndex);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}