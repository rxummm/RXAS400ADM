package com.rxas400adm.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单创建 DTO。
 */
@Data
public class PurchaseOrderCreateDTO {

    @NotBlank(message = "采购单号不能为空")
    @Size(max = 32, message = "采购单号最长32位")
    @Schema(description = "采购单号（手动输入或自动生成）")
    private String poNo;

    @Schema(description = "公司代码", defaultValue = "001")
    private String cono = "001";

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "供应商名称")
    private String vendorName;

    @Schema(description = "下单日期")
    private LocalDate orderDate;

    @Schema(description = "要求交货日期")
    private LocalDate reqDate;

    @Schema(description = "币种", defaultValue = "CNY")
    private String currency = "CNY";

    @Schema(description = "备注")
    private String notes;

    @NotEmpty(message = "订单行项不能为空")
    @Valid
    @Schema(description = "订单行项列表")
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {

        @Schema(description = "行号")
        private Integer lineNo;

        @Schema(description = "物料编码")
        private String itemCode;

        @Schema(description = "物料描述")
        private String itemDesc;

        @Schema(description = "单位", defaultValue = "EA")
        private String uom = "EA";

        @Schema(description = "订购数量")
        private BigDecimal qtyOrdered;

        @Schema(description = "单价")
        private BigDecimal unitPrice;

        @Schema(description = "要求到货日期")
        private LocalDate reqDate;

        @Schema(description = "行备注")
        private String notes;
    }
}