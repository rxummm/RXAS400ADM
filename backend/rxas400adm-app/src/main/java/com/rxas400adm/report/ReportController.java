package com.rxas400adm.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 报表引擎（3.11）：指标 / 执行记录 / 容量趋势报表，\n * 支持 format=xlsx|pdf 下载（Excel 中文无损；PDF 中文依赖系统 CJK 字体，缺失时数字/英文可读）。\n */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "报表中心")
public class ReportController {

    private final IReportService reportService;

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<byte[]> metrics(@RequestParam(defaultValue = "xlsx") String format,
                                          @RequestParam Long instanceId,
                                          @RequestParam(defaultValue = "7") int days) {
        ReportSpec spec = ReportSpec.metrics(instanceId, days);
        byte[] data = reportService.render(format, spec.title(), spec.headerArray(),
                reportService.metricsRows(instanceId, days));
        return file(format, "report-metrics", spec.title(), data);
    }

    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<byte[]> executions(@RequestParam(defaultValue = "xlsx") String format,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status) {
        ReportSpec spec = ReportSpec.executions();
        byte[] data = reportService.render(format, spec.title(), spec.headerArray(),
                reportService.executionRows(type, status));
        return file(format, "report-executions", spec.title(), data);
    }

    @GetMapping("/capacity")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<byte[]> capacity(@RequestParam(defaultValue = "xlsx") String format,
                                           @RequestParam Long instanceId,
                                           @RequestParam(defaultValue = "30") int days) {
        ReportSpec spec = ReportSpec.capacity(instanceId);
        byte[] data = reportService.render(format, spec.title(), spec.headerArray(),
                reportService.capacityRows(instanceId, days));
        return file(format, "report-capacity", spec.title(), data);
    }

    private ResponseEntity<byte[]> file(String format, String base, String title, byte[] data) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        String ext = pdf ? "pdf" : "xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(base + "-" + LocalDate.now() + "." + ext, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }


}