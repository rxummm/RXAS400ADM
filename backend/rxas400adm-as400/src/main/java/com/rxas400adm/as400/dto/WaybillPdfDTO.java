package com.rxas400adm.as400.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运单 PDF 导出参数 DTO（替代 Controller 层裸 Map）。
 */
@Data
public class WaybillPdfDTO {
    private String shipFrom;
    private String shipTo;
    private String carrier;
    private BigDecimal weight;
    private List<WaybillItem> items;
    private String notes;

    @Data
    public static class WaybillItem {
        private String itemCode;
        private String description;
        private Integer qty;
        private String unit;
    }
}
