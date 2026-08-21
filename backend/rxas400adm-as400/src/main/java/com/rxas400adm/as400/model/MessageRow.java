package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息描述行（QSYS2.MESSAGE_DESCRIPTION_INFO）。
 *
 * @param id          消息 ID（MESSAGE_ID）
 * @param text        消息文本（MESSAGE_TEXT）
 * @param secondLevel 二级文本（SECOND_LEVEL_TEXT）
 * @param severity    严重级别（SEVERITY）
 */
public record MessageRow(
        @JsonProperty("MESSAGE_ID") String id,
        @JsonProperty("MESSAGE_TEXT") String text,
        @JsonProperty("SECOND_LEVEL_TEXT") String secondLevel,
        @JsonProperty("SEVERITY") long severity) {
}
