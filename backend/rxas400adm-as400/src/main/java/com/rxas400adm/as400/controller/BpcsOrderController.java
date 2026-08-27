package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsOrderQueryDTO;
import com.rxas400adm.as400.service.IBpcsOrderService;
import com.rxas400adm.as400.vo.BpcsOrderHeaderVO;
import com.rxas400adm.as400.vo.BpcsOrderLineVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 【AS400 业务增强·P1】BPCS 客户订单时间轴（只读）。
 * 权限：BPCS_ORDER_VIEW；数据源 ECH/ECL 只读查询，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orders")
@RequiredArgsConstructor
@Tag(name = "BPCS客户订单")
public class BpcsOrderController {

    private final IBpcsOrderService bpcsOrderService;

    /** 订单头 + 进程时间轴 */
    @GetMapping("/header")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public ApiResponse<BpcsOrderHeaderVO> header(@Valid BpcsOrderQueryDTO query) {
        return ApiResponse.success(bpcsOrderService.getHeader(query));
    }

    /** 订单行全集（含各阶段数量/价格/仓库；行级状态推导） */
    @GetMapping("/lines")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public ApiResponse<List<BpcsOrderLineVO>> lines(@Valid BpcsOrderQueryDTO query) {
        return ApiResponse.success(bpcsOrderService.getLines(query));
    }
}
