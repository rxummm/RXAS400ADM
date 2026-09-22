package com.rxas400adm.tpm.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.tpm.entity.OeeRecord;
import com.rxas400adm.tpm.service.OeeService;
import com.rxas400adm.tpm.vo.OeeRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tpm/oee")
@RequiredArgsConstructor
@Tag(name = "设备综合效率", description = "OEE数据查询")
public class OeeController {

    private final OeeService service;

    @GetMapping
    @PreAuthorize("hasAuthority('OEE_VIEW')")
    @Operation(summary = "分页查询OEE记录")
    public ApiResponse<PageResult<OeeRecordVO>> page(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<OeeRecord> page = service.pageQuery(equipmentId, fromDate, toDate, current, size);
        return ApiResponse.success(page.map(OeeRecordVO::from));
    }

    @GetMapping("/latest/{equipmentId}")
    @PreAuthorize("hasAuthority('OEE_VIEW')")
    @Operation(summary = "获取设备最新OEE")
    public ApiResponse<OeeRecordVO> getLatest(@PathVariable Long equipmentId) {
        return ApiResponse.success(OeeRecordVO.from(service.getLatestByEquipment(equipmentId)));
    }
}
