package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * MSGW 消息条目（JobController.msgwMessages 返回）。
 */
public record MsgwMessageVO(Map<String, Object> data) {
    public static MsgwMessageVO from(Map<String, Object> map) {
        return new MsgwMessageVO(map);
    }
}
