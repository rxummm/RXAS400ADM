package com.rxas400adm.mrp.vo;

import com.rxas400adm.mrp.entity.BomMaster;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BomMasterVO {

    private Long id;
    private String bomNo;
    private String cono;
    private String parentItem;
    private String parentDesc;
    private String bomVersion;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static BomMasterVO from(BomMaster entity) {
        BomMasterVO vo = new BomMasterVO();
        vo.setId(entity.getId());
        vo.setBomNo(entity.getBomNo());
        vo.setCono(entity.getCono());
        vo.setParentItem(entity.getParentItem());
        vo.setParentDesc(entity.getParentDesc());
        vo.setBomVersion(entity.getBomVersion());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpiryDate(entity.getExpiryDate());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
