package com.rxas400adm.edi.vo;

import com.rxas400adm.edi.entity.EdiDocument;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EDI文档视图：与 EdiDocument 字段一致，隔离 Controller 直返 Entity。
 */
@Data
@NoArgsConstructor
public class EdiDocumentVO {

    private Long id;
    private String documentNo;
    private String cono;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private String documentType;
    private String direction;
    private String status;
    private String rawContent;
    private String parsedData;
    private String errorMessage;
    private LocalDateTime processedTime;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static EdiDocumentVO from(EdiDocument e) {
        EdiDocumentVO vo = new EdiDocumentVO();
        vo.setId(e.getId());
        vo.setDocumentNo(e.getDocumentNo());
        vo.setCono(e.getCono());
        vo.setPartnerId(e.getPartnerId());
        vo.setPartnerCode(e.getPartnerCode());
        vo.setPartnerName(e.getPartnerName());
        vo.setDocumentType(e.getDocumentType());
        vo.setDirection(e.getDirection());
        vo.setStatus(e.getStatus());
        vo.setRawContent(e.getRawContent());
        vo.setParsedData(e.getParsedData());
        vo.setErrorMessage(e.getErrorMessage());
        vo.setProcessedTime(e.getProcessedTime());
        vo.setCreatedBy(e.getCreatedBy());
        vo.setCreatedTime(e.getCreatedTime());
        vo.setUpdatedTime(e.getUpdatedTime());
        return vo;
    }
}
