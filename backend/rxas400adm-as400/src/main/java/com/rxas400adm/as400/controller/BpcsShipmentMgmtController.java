package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.WaybillPdfDTO;
import com.rxas400adm.as400.pdf.WaybillPdfRenderer;
import com.rxas400adm.as400.service.IBpcsShipmentMgmtService;
import com.rxas400adm.as400.vo.BpcsShipmentVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ㊳ 运单管理 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/shipment")
@RequiredArgsConstructor
@Tag(name = "BPCS Shipment Management", description = "Shipment and waybill management")
public class BpcsShipmentMgmtController {

    private final IBpcsShipmentMgmtService service;
    private final WaybillPdfRenderer pdfRenderer;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsShipmentVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listShipments(cono, limit));
    }

    /**
     * ㊼ 运输单 PDF 导出。
     *
     * @param waybillNo 运单号
     * @param params    运单数据（shipFrom/shipTo/carrier/weight/items/notes）
     * @return PDF 文件流
     */
    @PostMapping("/pdf")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @OperateLog(module = "BPCS运单管理", operation = "导出运单PDF")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String waybillNo,
            @RequestBody WaybillPdfDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put("shipFrom", dto.getShipFrom());
        params.put("shipTo", dto.getShipTo());
        params.put("carrier", dto.getCarrier());
        params.put("weight", dto.getWeight());
        params.put("notes", dto.getNotes());
        if (dto.getItems() != null) {
            List<Map<String, String>> items = new ArrayList<>();
            for (var item : dto.getItems()) {
                Map<String, String> m = new HashMap<>();
                m.put("itemCode", item.getItemCode());
                m.put("description", item.getDescription());
                m.put("qty", item.getQty() != null ? String.valueOf(item.getQty()) : "");
                m.put("unit", item.getUnit());
                items.add(m);
            }
            params.put("items", items);
        }
        byte[] pdf = pdfRenderer.render(waybillNo, params);
        String filename = "waybill-" + waybillNo + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
