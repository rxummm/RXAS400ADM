package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.DocVersion;

import java.time.LocalDateTime;

/**
 * 文档版本快照视图：与 DocVersion 字段契约解耦（不再直接暴露 Entity）。
 * P12 版本列表瘦身：不含 content 大字段，正文经 GET /docs/versions/{versionId}/content 懒加载。
 */
public record DocVersionVO(
        Long id,
        Long docId,
        Integer version,
        String title,
        String operator,
        LocalDateTime createdTime) {

    public static DocVersionVO from(DocVersion e) {
        return new DocVersionVO(
                e.getId(), e.getDocId(), e.getVersion(), e.getTitle(),
                e.getOperator(), e.getCreatedTime());
    }
}
