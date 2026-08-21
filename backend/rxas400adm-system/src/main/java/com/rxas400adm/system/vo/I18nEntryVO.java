package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.I18nEntry;

/**
 * 动态翻译视图：与 I18nEntry 字段契约解耦。
 */
public record I18nEntryVO(
        String i18nKey,
        String lang,
        String text) {

    public static I18nEntryVO from(I18nEntry e) {
        return new I18nEntryVO(e.getI18nKey(), e.getLang(), e.getText());
    }
}