package com.rxas400adm.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 采购订单查询 DTO。
 */
@Data
public class PurchaseOrderQueryDTO {

    @Schema(description = "采购单号（模糊）")
    private String poNo;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "供应商名称（模糊）")
    private String vendorName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "下单日期-开始")
    private LocalDate orderDateFrom;

    @Schema(description = "下单日期-结束")
    private LocalDate orderDateTo;

    @Schema(description = "页码", defaultValue = "1")
    private int current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private int size = 20;
}