package com.rxas400adm.finance.ar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录创建 DTO。
 */
@Data
public class ArPaymentCreateDTO {

    @Schema(description = "收款单号（手动输入或自动生成）")
    private String paymentNo;

    @Schema(description = "公司代码", defaultValue = "001")
    private String cono = "001";

    @NotNull(message = "关联发票ID不能为空")
    @Schema(description = "关联发票ID")
    private Long invoiceId;

    @NotNull(message = "收款日期不能为空")
    @Schema(description = "收款日期")
    private LocalDate paymentDate;

    @NotNull(message = "收款金额不能为空")
    @Schema(description = "收款金额")
    private BigDecimal amount;

    @Schema(description = "付款方式", defaultValue = "BANK_TRANSFER")
    private String paymentMethod = "BANK_TRANSFER";

    @Size(max = 64, message = "参考号最长64位")
    @Schema(description = "银行流水号/参考号")
    private String referenceNo;

    @Schema(description = "收款人")
    private String receivedBy;

    @Size(max = 500, message = "备注最长500位")
    @Schema(description = "备注")
    private String notes;
}
