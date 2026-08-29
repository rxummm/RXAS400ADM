package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.DocTemplate;

import java.time.LocalDateTime;

/**
 * 文档模板视图：与 DocTemplate 字段契约解耦。
 */
public record DocTemplateVO(
        Long id,
        String name,
        String category,
        String content,
        String docType,
        String createdBy,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static DocTemplateVO from(DocTemplate e) {
        return new DocTemplateVO(
                e.getId(), e.getName(), e.getCategory(), e.getContent(), e.getDocType(),
                e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
