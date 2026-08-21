package com.rxas400adm.compile.vo;

import com.rxas400adm.compile.entity.CompileRecord;

import java.time.LocalDateTime;

/**
 * 编译记录视图（P2-10）：与 CompileRecord 字段一致。
 */
public record CompileRecordVO(
        Long id,
        String library,
        String sourceFile,
        String member,
        String command,
        String status,
        String message,
        String operator,
        LocalDateTime createdTime) {

    public static CompileRecordVO from(CompileRecord e) {
        return new CompileRecordVO(
                e.getId(), e.getLibrary(), e.getSourceFile(), e.getMember(), e.getCommand(),
                e.getStatus(), e.getMessage(), e.getOperator(), e.getCreatedTime());
    }
}
