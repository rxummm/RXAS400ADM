package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.Doc;

import java.time.LocalDateTime;

/**
 * 运维文档视图：与 Doc 字段契约解耦。
 * 剔除审计字段（createdBy/updatedBy/approvedBy）和逻辑删除字段（deleted/deletedTime）。
 */
public record DocVO(
        Long id,
        Long templateId,
        String title,
        String content,
        String docType,
        Integer version,
        String status,
        String rejectReason,
        String ifsPath,
        String approvedBy,
        LocalDateTime approvedTime,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static DocVO from(Doc e) {
        return new DocVO(
                e.getId(), e.getTemplateId(), e.getTitle(), e.getContent(), e.getDocType(),
                e.getVersion(), e.getStatus(), e.getRejectReason(), e.getIfsPath(),
                e.getApprovedBy(), e.getApprovedTime(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
