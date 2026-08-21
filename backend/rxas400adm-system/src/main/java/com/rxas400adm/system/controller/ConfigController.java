package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.dto.SysConfigDTO;
import com.rxas400adm.system.service.ISysConfigService;
import com.rxas400adm.system.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统参数管理（rx_config）：参数列表 / 修改 / 删除。
 * 权限码：SYS_CONFIG_MANAGE。
 * R1 分层清零：Mapper 已下沉至 ISysConfigService（掩码逻辑在服务内，P2-6）。
 */
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Tag(name = "系统参数")
public class ConfigController {

    private final ISysConfigService configService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYS_CONFIG_MANAGE')")
    public ApiResponse<List<SysConfigVO>> list() {
        return ApiResponse.success(configService.list().stream().map(SysConfigVO::from).toList());
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("hasAuthority('SYS_CONFIG_MANAGE')")
    @OperateLog(module = "系统配置", operation = "修改系统参数")
    public ApiResponse<SysConfigVO> update(@PathVariable String configKey, @Valid @RequestBody SysConfigDTO dto) {
        return ApiResponse.success(SysConfigVO.from(configService.update(configKey, dto)));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("hasAuthority('SYS_CONFIG_MANAGE')")
    @OperateLog(module = "系统配置", operation = "删除系统参数")
    public ApiResponse<Void> delete(@PathVariable String configKey) {
        configService.delete(configKey);
        return ApiResponse.success(null);
    }
}
