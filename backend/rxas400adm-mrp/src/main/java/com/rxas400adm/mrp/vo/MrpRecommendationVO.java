package com.rxas400adm.mrp.vo;

import com.rxas400adm.mrp.entity.MrpRecommendation;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MrpRecommendationVO {

    private Long id;
    private String recommendationNo;
    private String cono;
    private Long demandId;
    private String itemCode;
    private String itemDesc;
    private BigDecimal recommendQty;
    private String recommendType;
    private Integer leadTimeDays;
    private LocalDate suggestedDate;
    private String orderNo;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;

    public static MrpRecommendationVO from(MrpRecommendation entity) {
        MrpRecommendationVO vo = new MrpRecommendationVO();
        vo.setId(entity.getId());
        vo.setRecommendationNo(entity.getRecommendationNo());
        vo.setCono(entity.getCono());
        vo.setDemandId(entity.getDemandId());
        vo.setItemCode(entity.getItemCode());
        vo.setItemDesc(entity.getItemDesc());
        vo.setRecommendQty(entity.getRecommendQty());
        vo.setRecommendType(entity.getRecommendType());
        vo.setLeadTimeDays(entity.getLeadTimeDays());
        vo.setSuggestedDate(entity.getSuggestedDate());
        vo.setOrderNo(entity.getOrderNo());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
