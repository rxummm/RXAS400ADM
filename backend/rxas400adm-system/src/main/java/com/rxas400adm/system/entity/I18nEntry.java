package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 动态翻译（rx_i18n，2.5.2 轻量版）：管理端可维护的 i18n key → 各语言文案。
 */
@Data
@TableName("rx_i18n")
public class I18nEntry {

    private String i18nKey;

    private String lang;

    private String text;
}
