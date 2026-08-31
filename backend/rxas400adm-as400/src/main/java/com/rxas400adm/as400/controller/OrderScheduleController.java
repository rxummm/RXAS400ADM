package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.OrderScheduleDTO;
import com.rxas400adm.as400.service.IOrderScheduleService;
import com.rxas400adm.as400.vo.OrderScheduleVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单排程视图 Controller（CRUD）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderSchedule")
@RequiredArgsConstructor
public class OrderScheduleController {

    private final IOrderScheduleService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<OrderScheduleVO>> list(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(service.list(startDate, endDate).stream().map(OrderScheduleVO::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<OrderScheduleVO> get(@PathVariable Long id) {
        return ApiResponse.success(OrderScheduleVO.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "创建订单排程")
    public ApiResponse<OrderScheduleVO> create(@RequestBody OrderScheduleDTO dto) {
        return ApiResponse.success(OrderScheduleVO.from(service.create(dto, "admin")));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "更新订单排程")
    public ApiResponse<OrderScheduleVO> update(@RequestBody OrderScheduleDTO dto) {
        return ApiResponse.success(OrderScheduleVO.from(service.update(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "删除订单排程")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
