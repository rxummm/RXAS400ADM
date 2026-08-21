package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息文件（*MSGF）行。
 *
 * @param name     消息文件名（MESSAGE_FILE_NAME）
 * @param library  消息文件库（MESSAGE_FILE_LIBRARY）
 * @param messages 消息数（NUMBER_OF_MESSAGES）
 * @param text     描述（MESSAGE_FILE_TEXT）
 */
public record MessageFileRow(
        @JsonProperty("MESSAGE_FILE_NAME") String name,
        @JsonProperty("MESSAGE_FILE_LIBRARY") String library,
        @JsonProperty("NUMBER_OF_MESSAGES") long messages,
        @JsonProperty("MESSAGE_FILE_TEXT") String text) {
}
