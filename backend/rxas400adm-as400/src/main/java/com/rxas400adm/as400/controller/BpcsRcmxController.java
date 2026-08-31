package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsRcmxConfigDTO;
import com.rxas400adm.as400.dto.BpcsRcmxImportResult;
import com.rxas400adm.as400.service.IBpcsRcmxService;
import com.rxas400adm.as400.vo.BpcsRcmxAssignmentVO;
import com.rxas400adm.as400.vo.BpcsCsrOptionVO;
import com.rxas400adm.as400.vo.BpcsCustOptionVO;
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
 * ㊾ RCMX 客户 CSR 分配管理 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/rcmx")
@RequiredArgsConstructor
public class BpcsRcmxController {

    private final IBpcsRcmxService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsRcmxAssignmentVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(required = false) String custLike,
            @RequestParam(required = false) String csrLike,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listAssignments(cono, custLike, csrLike, limit));
    }

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<BpcsRcmxAssignmentVO> get(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String cust) {
        return ApiResponse.success(service.getAssignment(cono, cust));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    public ApiResponse<Void> create(
            @RequestParam(defaultValue = "001") String cono,
            @Valid @RequestBody BpcsRcmxConfigDTO dto) {
        service.createAssignment(cono, dto);
        return ApiResponse.success(null);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    public ApiResponse<Void> update(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String cust,
            @Valid @RequestBody BpcsRcmxConfigDTO dto) {
        service.updateAssignment(cono, cust, dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    public ApiResponse<Void> delete(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String cust) {
        service.deleteAssignment(cono, cust);
        return ApiResponse.success(null);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsCustOptionVO>> customers(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.searchCustomers(cono, keyword));
    }

    @GetMapping("/csrOptions")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsCsrOptionVO>> csrOptions(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.searchCsrOptions(cono, keyword));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    public ApiResponse<BpcsRcmxImportResult> importExcel(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        List<BpcsRcmxConfigDTO> list = parseExcel(file);
        return ApiResponse.success(service.importAssignments(cono, list));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsRcmxAssignmentVO>> export(
            @RequestParam(defaultValue = "001") String cono) {
        return ApiResponse.success(service.exportAssignments(cono));
    }

    private List<BpcsRcmxConfigDTO> parseExcel(MultipartFile file) throws IOException {
        List<BpcsRcmxConfigDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                BpcsRcmxConfigDTO dto = new BpcsRcmxConfigDTO();
                dto.setCust(getCellString(row, 0));
                dto.setCsrId(getCellString(row, 1));
                dto.setActive(getCellString(row, 2));
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
}