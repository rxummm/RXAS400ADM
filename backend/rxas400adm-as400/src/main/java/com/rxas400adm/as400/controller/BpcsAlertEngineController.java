package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsAlertEngineService;
import com.rxas400adm.as400.vo.BpcsAlertRuleVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * ㉜ 预警规则引擎 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/alert")
@RequiredArgsConstructor
@Tag(name = "BPCS Alert Engine", description = "Alert engine monitoring and processing")
public class BpcsAlertEngineController {

    private final IBpcsAlertEngineService service;

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsAlertRuleVO>> rules(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.getAlertRules(cono, limit));
    }
}
