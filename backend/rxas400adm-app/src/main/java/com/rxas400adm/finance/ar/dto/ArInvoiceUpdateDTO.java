package com.rxas400adm.finance.ar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款发票更新 DTO。
 */
@Data
public class ArInvoiceUpdateDTO {

    @Size(max = 32, message = "账单号最长32位")
    @Schema(description = "账单号")
    private String invoiceNo;

    @Schema(description = "公司代码")
    private String cono;

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "来源采购单号")
    private String originPoNo;

    @Schema(description = "来源销售单号")
    private String originSoNo;

    @Schema(description = "小计")
    private BigDecimal subtotal;

    @Schema(description = "税率")
    private BigDecimal taxRate;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "备注")
    private String notes;
}
