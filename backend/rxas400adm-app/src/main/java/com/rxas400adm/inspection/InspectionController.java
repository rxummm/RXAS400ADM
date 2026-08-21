package com.rxas400adm.inspection;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.inspection.vo.InspectionResultVO;
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
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 巡检报告（3.x 借鉴旧项目 inspectionReport）：综合健康巡检 + 导出（xlsx/pdf）。
 * 视图/导出均只读，权限复用 MONITOR_VIEW。
 */
@RestController
@RequestMapping("/api/v1/inspection")
@RequiredArgsConstructor
@Tag(name = "巡检报告")
public class InspectionController {

    private final IInspectionService inspectionService;

    @GetMapping("/generate")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ApiResponse<InspectionResultVO> generate(@RequestParam Long serverId) {
        return ApiResponse.success(InspectionResultVO.from(inspectionService.generate(serverId)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('MONITOR_VIEW')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam Long serverId) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        String ext = pdf ? "pdf" : "xlsx";
        byte[] data = inspectionService.export(format, serverId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("inspection-" + serverId + "-" + LocalDate.now() + "." + ext, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}