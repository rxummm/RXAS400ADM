package com.rxas400adm.mrp.vo;

import com.rxas400adm.mrp.entity.BomLine;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BomLineVO {

    private Long id;
    private Long bomId;
    private Integer lineNo;
    private String componentItem;
    private String componentDesc;
    private BigDecimal quantity;
    private String uom;
    private BigDecimal scrapRate;
    private BigDecimal efficiency;
    private LocalDateTime createdTime;

    public static BomLineVO from(BomLine entity) {
        BomLineVO vo = new BomLineVO();
        vo.setId(entity.getId());
        vo.setBomId(entity.getBomId());
        vo.setLineNo(entity.getLineNo());
        vo.setComponentItem(entity.getComponentItem());
        vo.setComponentDesc(entity.getComponentDesc());
        vo.setQuantity(entity.getQuantity());
        vo.setUom(entity.getUom());
        vo.setScrapRate(entity.getScrapRate());
        vo.setEfficiency(entity.getEfficiency());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
