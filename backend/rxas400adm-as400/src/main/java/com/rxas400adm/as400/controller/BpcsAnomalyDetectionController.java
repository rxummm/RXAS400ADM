package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsAnomalyDetectionService;
import com.rxas400adm.as400.vo.BpcsOrderAnomalyVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ⑨ 异常检测引擎 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/anomaly")
@RequiredArgsConstructor
public class BpcsAnomalyDetectionController {

    private final IBpcsAnomalyDetectionService service;

    @GetMapping("/detect")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsOrderAnomalyVO>> detect(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.detectAnomalies(cono, limit));
    }
}
