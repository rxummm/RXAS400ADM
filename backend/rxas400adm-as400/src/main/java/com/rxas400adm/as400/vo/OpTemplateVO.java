package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.OpTemplate;

import java.time.LocalDateTime;

/**
 * 操作模板视图（不暴露审计内部字段语义，API 形状与前端约定一致）。
 */
public record OpTemplateVO(
        Long id,
        String name,
        String description,
        String steps,
        String createdBy,
        LocalDateTime createdTime,
        String updatedBy,
        LocalDateTime updatedTime) {

    public static OpTemplateVO from(OpTemplate e) {
        return new OpTemplateVO(e.getId(), e.getName(), e.getDescription(), e.getSteps(),
                e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedBy(), e.getUpdatedTime());
    }
}
