package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import com.rxas400adm.as400.service.IPfService;
import com.rxas400adm.as400.vo.PfDataVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * PF 物理文件浏览（2.4.5）。
 */
@RestController
@RequestMapping("/api/v1/pf")
@RequiredArgsConstructor
@Tag(name = "物理文件")
public class PfController {

    private final IPfService pfService;

    @GetMapping("/files")
    @PreAuthorize("hasAuthority('PF_VIEW')")
    public ApiResponse<List<PfRow>> files(@RequestParam(defaultValue = "APP") String library) {
        return ApiResponse.success(pfService.files(library));
    }

    @GetMapping("/columns")
    @PreAuthorize("hasAuthority('PF_VIEW')")
    public ApiResponse<List<PfColumnRow>> columns(@RequestParam String library, @RequestParam String file) {
        return ApiResponse.success(pfService.columns(library, file));
    }

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('PF_VIEW')")
    public ApiResponse<List<PfDataVO>> data(@RequestParam String library,
                                              @RequestParam String file,
                                              @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(pfService.data(library, file, limit).stream()
                .map(PfDataVO::from).toList());
    }
}
