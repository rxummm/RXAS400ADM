package com.rxas400adm.edi.vo;

import com.rxas400adm.edi.entity.EdiPartner;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EDI伙伴视图：与 EdiPartner 字段一致，隔离 Controller 直返 Entity。
 */
@Data
@NoArgsConstructor
public class EdiPartnerVO {

    private Long id;
    private String partnerCode;
    private String partnerName;
    private String partnerType;
    private String ediVersion;
    private String as2Url;
    private String as2FromId;
    private String as2ToId;
    private String as2Micalg;
    private String as2Encalgo;
    private String as2CertPath;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static EdiPartnerVO from(EdiPartner e) {
        EdiPartnerVO vo = new EdiPartnerVO();
        vo.setId(e.getId());
        vo.setPartnerCode(e.getPartnerCode());
        vo.setPartnerName(e.getPartnerName());
        vo.setPartnerType(e.getPartnerType());
        vo.setEdiVersion(e.getEdiVersion());
        vo.setAs2Url(e.getAs2Url());
        vo.setAs2FromId(e.getAs2FromId());
        vo.setAs2ToId(e.getAs2ToId());
        vo.setAs2Micalg(e.getAs2Micalg());
        vo.setAs2Encalgo(e.getAs2Encalgo());
        vo.setAs2CertPath(e.getAs2CertPath());
        vo.setStatus(e.getStatus());
        vo.setCreatedBy(e.getCreatedBy());
        vo.setCreatedTime(e.getCreatedTime());
        vo.setUpdatedTime(e.getUpdatedTime());
        return vo;
    }
}
