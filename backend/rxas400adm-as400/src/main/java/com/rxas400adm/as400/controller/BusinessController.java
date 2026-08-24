package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBusinessService;
import com.rxas400adm.as400.vo.ColumnDetailVO;
import com.rxas400adm.as400.vo.TableDataVO;
import com.rxas400adm.as400.vo.TableInfoVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 通用业务数据查询（借鉴旧项目 biz 订单/SKU/价格 + schemaCrud 字段定义，通用化）：
 * - /tables  库内文件清单（QSYS2.SYSTABLES）
 * - /columns 文件字段定义（QSYS2.SYSCOLUMNS，含长度/描述）
 * - /data    业务数据分页浏览（关键词模糊查询，仅字符型字段）
 * 数据源按 X-AS400-Server 头路由当前服务器；全部为只读查询，权限复用 QUERY_EXECUTE。
 */
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Tag(name = "数据查询")
public class BusinessController {

    private final IBusinessService businessService;

    @GetMapping("/tables")
    @PreAuthorize("hasAuthority('QUERY_EXECUTE')")
    public ApiResponse<List<TableInfoVO>> tables(@RequestParam(required = false) String library,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.success(businessService.tables(library, keyword).stream()
                .map(m -> new TableInfoVO(
                        String.valueOf(m.getOrDefault("TABLE_NAME", "")),
                        String.valueOf(m.getOrDefault("TABLE_TEXT", "")),
                        m.get("ROWS") instanceof Number n ? n.longValue() : 0L))
                .toList());
    }

    @GetMapping("/columns")
    @PreAuthorize("hasAuthority('QUERY_EXECUTE')")
    public ApiResponse<List<ColumnDetailVO>> columns(@RequestParam String library,
                                                     @RequestParam String table) {
        return ApiResponse.success(businessService.columns(library, table).stream()
                .map(ColumnDetailVO::from).toList());
    }

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('QUERY_EXECUTE')")
    // 服务层返回遗留 Map 载荷，columns/rows 需一次性收窄；方法级抑制以覆盖构造器实参中的两处受检转换
    @SuppressWarnings("unchecked")
    public ApiResponse<TableDataVO> data(@RequestParam String library,
                                         @RequestParam String table,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> raw = businessService.data(library, table, keyword, page, size);
        return ApiResponse.success(new TableDataVO(
                raw.get("total") instanceof Number n ? n.longValue() : 0L,
                raw.get("columns") instanceof List<?> c ? (List<String>) (List<?>) c : List.of(),
                raw.get("rows") instanceof List<?> r ? (List<Map<String, Object>>) (List<?>) r : List.of()));
    }
}
