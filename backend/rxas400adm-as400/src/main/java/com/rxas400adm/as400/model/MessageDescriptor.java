package com.rxas400adm.as400.model;

/**
 * 消息描述增改参数对象（中-6）：ADDMSGD/CHGMSGD 六参签名收敛。
 *
 * @param library     消息文件库（QSYS 等）
 * @param file        消息文件名
 * @param id          消息 ID（如 USR9001）
 * @param text        一级消息文本
 * @param secondLevel 二级说明文本（可空）
 * @param severity    严重级别 0-99
 */
public record MessageDescriptor(String library, String file, String id,
                                String text, String secondLevel, int severity) {
}
