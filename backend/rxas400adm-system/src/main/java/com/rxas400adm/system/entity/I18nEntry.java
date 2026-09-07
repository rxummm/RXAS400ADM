package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态翻译（rx_i18n）：管理端可维护的 i18n key → 各语言文案。
 */
@Data
@TableName("rx_i18n")
public class I18nEntry {

    private String i18nKey;

    private String lang;

    private String text;

    private String module;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    private String updatedBy;
}
