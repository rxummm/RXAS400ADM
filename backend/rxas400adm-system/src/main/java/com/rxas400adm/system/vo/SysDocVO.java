package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.SysDoc;

import java.time.LocalDateTime;

/**
 * 知识库文档视图：与 SysDoc 字段契约解耦。
 */
public record SysDocVO(
        Long id,
        String title,
        String content,
        String category,
        String tags,
        String status,
        String createdBy,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static SysDocVO from(SysDoc e) {
        return new SysDocVO(
                e.getId(), e.getTitle(), e.getContent(), e.getCategory(), e.getTags(),
                e.getStatus(), e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
