package com.rxas400adm.finance.ar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录表（rx_ar_payment）。
 * 记录客户付款信息，关联发票进行核销。
 */
@Data
@TableName("rx_ar_payment")
public class ArPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收款单号（PMT + yyyyMMdd + 4位序号） */
    private String paymentNo;

    /** 公司代码 */
    private String cono;

    /** 关联发票 ID */
    private Long invoiceId;

    /** 收款日期 */
    private LocalDate paymentDate;

    /** 收款金额 */
    private BigDecimal amount;

    /** 付款方式：BANK_TRANSFER / CASH / CHECK / CREDIT_CARD / OTHER */
    private String paymentMethod;

    /** 银行流水号/参考号 */
    private String referenceNo;

    /** 收款人 */
    private String receivedBy;

    /** 备注 */
    private String notes;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
