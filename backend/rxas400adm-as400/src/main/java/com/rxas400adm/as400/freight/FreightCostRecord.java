package com.rxas400adm.as400.freight;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rx_freight_cost_record")
public class FreightCostRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String carrier;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer pieceCount;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal costDiff;
    private LocalDate shipDate;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
