package com.rxas400adm.report.builder;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.report.builder.dto.ReportDefinitionDTO;
import com.rxas400adm.report.builder.vo.DataSourceMeta;
import com.rxas400adm.report.builder.vo.ReportDefinitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.rxas400adm.common.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 自定义报表构建器 REST API。
 */
@RestController
@RequestMapping("/api/v1/report-builder")
@RequiredArgsConstructor
@Tag(name = "自定义报表构建器")
public class ReportBuilderController {

    private final IReportBuilderService reportBuilderService;

    @GetMapping("/datasources")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_VIEW')")
    @Operation(summary = "查询全部数据源及其可用字段")
    public ApiResponse<List<DataSourceMeta>> listDataSources() {
        return ApiResponse.success(reportBuilderService.listDataSources());
    }

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_VIEW')")
    @Operation(summary = "查询全部报表定义")
    public ApiResponse<List<ReportDefinitionVO>> listDefinitions() {
        return ApiResponse.success(reportBuilderService.listDefinitions());
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_VIEW')")
    @Operation(summary = "查询单个报表定义")
    public ApiResponse<ReportDefinitionVO> getDefinition(@PathVariable Long id) {
        return ApiResponse.success(reportBuilderService.getDefinition(id));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_MANAGE')")
    @OperateLog(module = "报表构建器", operation = "创建报表定义")
    @Operation(summary = "创建报表定义")
    public ApiResponse<ReportDefinitionVO> createDefinition(
            @Valid @RequestBody ReportDefinitionDTO dto) {
        return ApiResponse.success(reportBuilderService.createDefinition(dto, SecurityUtils.currentUsername()));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_MANAGE')")
    @OperateLog(module = "报表构建器", operation = "更新报表定义")
    @Operation(summary = "更新报表定义")
    public ApiResponse<ReportDefinitionVO> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody ReportDefinitionDTO dto) {
        return ApiResponse.success(reportBuilderService.updateDefinition(id, dto));
    }

    @DeleteMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_MANAGE')")
    @OperateLog(module = "报表构建器", operation = "删除报表定义")
    @Operation(summary = "删除报表定义")
    public ApiResponse<Void> deleteDefinition(@PathVariable Long id) {
        reportBuilderService.deleteDefinition(id);
        return ApiResponse.success();
    }

    @GetMapping("/definitions/{id}/execute")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_VIEW')")
    @Operation(summary = "执行报表（预览）")
    public ApiResponse<Map<String, Object>> executeReport(@PathVariable Long id) {
        return ApiResponse.success(reportBuilderService.executeReport(id));
    }

    @GetMapping("/definitions/{id}/export")
    @PreAuthorize("hasAuthority('REPORT_BUILDER_VIEW')")
    @Operation(summary = "导出报表（Excel/PDF）")
    public void exportReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "xlsx") String format,
            HttpServletResponse response) throws Exception {
        byte[] data = reportBuilderService.exportReport(id, format);
        String ext = "pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx";
        String filename = URLEncoder.encode("report-" + LocalDate.now() + "." + ext, StandardCharsets.UTF_8);
        response.setContentType("pdf".equalsIgnoreCase(ext)
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
