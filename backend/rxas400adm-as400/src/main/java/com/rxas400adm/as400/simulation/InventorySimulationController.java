package com.rxas400adm.as400.simulation;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bpcs/inventory-simulation")
@RequiredArgsConstructor
@Tag(name = "库存模拟")
public class InventorySimulationController {

    private final InventorySimulationService simulationService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "分页查询模拟记录")
    public ApiResponse<Map<String, Object>> list(SimulationQueryDTO query) {
        var page = simulationService.list(query);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(InventorySimulationVO::from).toList());
        result.put("total", page.getTotal());
        return ApiResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "库存模拟", operation = "创建模拟")
    @Operation(summary = "创建库存模拟")
    public ApiResponse<InventorySimulationVO> create(@Valid @RequestBody SimulationCreateDTO dto) {
        return ApiResponse.success(InventorySimulationVO.from(simulationService.create(dto)));
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "库存模拟", operation = "执行模拟")
    @Operation(summary = "执行库存模拟")
    public ApiResponse<InventorySimulationVO> run(@PathVariable Long id) {
        return ApiResponse.success(InventorySimulationVO.from(simulationService.run(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "库存模拟", operation = "删除模拟")
    @Operation(summary = "删除模拟记录")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        simulationService.delete(id);
        return ApiResponse.success(null);
    }
}
