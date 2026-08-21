package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命令脚本中心（rx_command_script，2.4.3）：保存/复用 CL 命令，含收藏与标签。
 */
@Data
@TableName("rx_command_script")
public class CommandScript {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** CL 命令 */
    private String command;

    private Boolean favorite;

    /** 逗号分隔标签 */
    private String tags;

    private String createdBy;

    private Integer runCount;

    private LocalDateTime lastRunTime;

    private String lastResult;

    /** M1：结构化执行状态 SUCCESS/FAILED（不再依赖解析 lastResult 中文字串） */
    private String lastRunStatus;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
