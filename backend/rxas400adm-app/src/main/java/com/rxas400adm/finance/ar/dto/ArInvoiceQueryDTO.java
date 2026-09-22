package com.rxas400adm.finance.ar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 应收账款发票查询 DTO。
 */
@Data
public class ArInvoiceQueryDTO {

    @Schema(description = "账单号（模糊）")
    private String invoiceNo;

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "客户名称（模糊）")
    private String customerName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "开票日期-开始")
    private LocalDate invoiceDateFrom;

    @Schema(description = "开票日期-结束")
    private LocalDate invoiceDateTo;

    @Schema(description = "到期日期-开始")
    private LocalDate dueDateFrom;

    @Schema(description = "到期日期-结束")
    private LocalDate dueDateTo;

    @Schema(description = "页码", defaultValue = "1")
    private int current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private int size = 20;
}
