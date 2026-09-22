package com.rxas400adm.mrp.vo;

import com.rxas400adm.mrp.entity.MrpDemand;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MrpDemandVO {

    private Long id;
    private String demandNo;
    private String cono;
    private String demandType;
    private String demandSource;
    private String itemCode;
    private String itemDesc;
    private BigDecimal grossRequirement;
    private BigDecimal scheduledReceipt;
    private BigDecimal allocated;
    private BigDecimal onHand;
    private BigDecimal safetyStock;
    private BigDecimal netRequirement;
    private LocalDate requiredDate;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;

    public static MrpDemandVO from(MrpDemand entity) {
        MrpDemandVO vo = new MrpDemandVO();
        vo.setId(entity.getId());
        vo.setDemandNo(entity.getDemandNo());
        vo.setCono(entity.getCono());
        vo.setDemandType(entity.getDemandType());
        vo.setDemandSource(entity.getDemandSource());
        vo.setItemCode(entity.getItemCode());
        vo.setItemDesc(entity.getItemDesc());
        vo.setGrossRequirement(entity.getGrossRequirement());
        vo.setScheduledReceipt(entity.getScheduledReceipt());
        vo.setAllocated(entity.getAllocated());
        vo.setOnHand(entity.getOnHand());
        vo.setSafetyStock(entity.getSafetyStock());
        vo.setNetRequirement(entity.getNetRequirement());
        vo.setRequiredDate(entity.getRequiredDate());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
