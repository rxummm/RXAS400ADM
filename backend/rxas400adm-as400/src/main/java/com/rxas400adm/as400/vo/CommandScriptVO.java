package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.CommandScript;

import java.time.LocalDateTime;

/**
 * 命令脚本视图（P2-10）：字段与 CommandScript 一致，API 契约与表结构解耦。
 */
public record CommandScriptVO(
        Long id,
        String name,
        String description,
        String command,
        Boolean favorite,
        String tags,
        String createdBy,
        Integer runCount,
        LocalDateTime lastRunTime,
        String lastResult,
        String lastRunStatus,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static CommandScriptVO from(CommandScript e) {
        return new CommandScriptVO(
                e.getId(), e.getName(), e.getDescription(), e.getCommand(), e.getFavorite(), e.getTags(),
                e.getCreatedBy(), e.getRunCount(), e.getLastRunTime(), e.getLastResult(), e.getLastRunStatus(),
                e.getCreatedTime(), e.getUpdatedTime());
    }
}
