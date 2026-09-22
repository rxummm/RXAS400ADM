package com.rxas400adm.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单更新 DTO。
 * 仅 DRAFT 状态可更新，行项全量替换。
 */
@Data
public class PurchaseOrderUpdateDTO {

    @Size(max = 32, message = "采购单号最长32位")
    @Schema(description = "采购单号")
    private String poNo;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "供应商名称")
    private String vendorName;

    @Schema(description = "下单日期")
    private LocalDate orderDate;

    @Schema(description = "要求交货日期")
    private LocalDate reqDate;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "备注")
    private String notes;

    @Valid
    @Schema(description = "订单行项列表（全量替换）")
    private List<PurchaseOrderCreateDTO.ItemDTO> items;
}