package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.service.ITopologyService;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 调用拓扑（3.7）：库级对象引用关系图（nodes + links）。\n * 前端 ECharts 关系图渲染，点击节点可下钻对象详情/引用（复用 /objects 接口）。\n */
@RestController
@RequestMapping("/api/v1/topology")
@RequiredArgsConstructor
@Tag(name = "拓扑视图")
public class TopologyController {

    private final ITopologyService topologyService;

    @GetMapping
    @PreAuthorize("hasAuthority('TOPOLOGY_VIEW')")
    public ApiResponse<GraphData> graph(@RequestParam(defaultValue = "APP") String library) {
        return ApiResponse.success(topologyService.graph(library));
    }
}
