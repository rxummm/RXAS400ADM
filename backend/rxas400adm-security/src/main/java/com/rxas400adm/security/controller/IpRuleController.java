package com.rxas400adm.security.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.dto.IpRuleCreateDTO;
import com.rxas400adm.security.dto.IpRuleUpdateDTO;
import com.rxas400adm.security.entity.IpRule;
import com.rxas400adm.security.service.IIpRuleService;
import com.rxas400adm.security.vo.IpRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 登录 IP 黑白名单管理（rx_ip_rule）。权限码：SYS_IP_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/ip-rules")
@RequiredArgsConstructor
@Tag(name = "IP规则")
public class IpRuleController {

    private final IIpRuleService ipRuleService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('SYS_IP_MANAGE')")
    public ApiResponse<PageResult<IpRuleVO>> page(@RequestParam(defaultValue = "1") int current,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String keyword) {
        PageResult<IpRule> page = ipRuleService.page(current, size, type, keyword);
        return ApiResponse.success(new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(IpRuleVO::from).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYS_IP_MANAGE')")
    @OperateLog(module = "登录安全", operation = "新增 IP 规则")
    public ApiResponse<IpRuleVO> create(@Valid @RequestBody IpRuleCreateDTO dto) {
        return ApiResponse.success(IpRuleVO.from(ipRuleService.create(dto, currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYS_IP_MANAGE')")
    @OperateLog(module = "登录安全", operation = "修改 IP 规则")
    public ApiResponse<IpRuleVO> update(@PathVariable Long id, @Valid @RequestBody IpRuleUpdateDTO dto) {
        return ApiResponse.success(IpRuleVO.from(ipRuleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYS_IP_MANAGE')")
    @OperateLog(module = "登录安全", operation = "删除 IP 规则")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ipRuleService.delete(id);
        return ApiResponse.success(null);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}