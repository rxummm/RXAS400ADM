package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.DocVersion;

import java.time.LocalDateTime;

/**
 * 文档版本快照视图：与 DocVersion 字段契约解耦（不再直接暴露 Entity）。
 */
public record DocVersionVO(
        Long id,
        Long docId,
        Integer version,
        String title,
        String content,
        String operator,
        LocalDateTime createdTime) {

    public static DocVersionVO from(DocVersion e) {
        return new DocVersionVO(
                e.getId(), e.getDocId(), e.getVersion(), e.getTitle(), e.getContent(),
                e.getOperator(), e.getCreatedTime());
    }
}