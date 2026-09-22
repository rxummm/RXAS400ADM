package com.rxas400adm.finance.ar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款发票创建 DTO。
 */
@Data
public class ArInvoiceCreateDTO {

    @NotBlank(message = "账单号不能为空")
    @Size(max = 32, message = "账单号最长32位")
    @Schema(description = "账单号（手动输入或自动生成）")
    private String invoiceNo;

    @Schema(description = "公司代码", defaultValue = "001")
    private String cono = "001";

    @NotBlank(message = "客户编码不能为空")
    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "客户名称")
    private String customerName;

    @NotNull(message = "开票日期不能为空")
    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @NotNull(message = "到期日期不能为空")
    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "来源采购单号")
    private String originPoNo;

    @Schema(description = "来源销售单号")
    private String originSoNo;

    @NotNull(message = "小计不能为空")
    @Schema(description = "小计")
    private BigDecimal subtotal;

    @Schema(description = "税率", defaultValue = "0.0000")
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Schema(description = "税额")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull(message = "总金额不能为空")
    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "币种", defaultValue = "CNY")
    private String currency = "CNY";

    @Schema(description = "备注")
    private String notes;
}
