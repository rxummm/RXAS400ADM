package com.rxas400adm.finance.ar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款发票表（rx_ar_invoice）。
 * 支持客户账单、收款核销、账龄分析。
 */
@Data
@TableName("rx_ar_invoice")
public class ArInvoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账单号（AR + yyyyMMdd + 4位序号） */
    private String invoiceNo;

    /** 公司代码 */
    private String cono;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 开票日期 */
    private LocalDate invoiceDate;

    /** 到期日期 */
    private LocalDate dueDate;

    /** 来源采购单号（关联 PO） */
    private String originPoNo;

    /** 来源销售单号 */
    private String originSoNo;

    /** 小计 */
    private BigDecimal subtotal;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 未结余额 */
    private BigDecimal balance;

    /** 币种 */
    private String currency;

    /**
     * 状态：DRAFT / OPEN / PARTIAL / PAID / OVERDUE / WRITE_OFF
     */
    private String status;

    /** 账龄区间：0-30 / 31-60 / 61-90 / 90+ */
    private String agingBucket;

    /** 备注 */
    private String notes;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
