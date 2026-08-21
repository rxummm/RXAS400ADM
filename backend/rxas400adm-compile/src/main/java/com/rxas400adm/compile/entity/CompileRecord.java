package com.rxas400adm.compile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编译记录表（rx_compile_record）
 */
@Data
@TableName("rx_compile_record")
public class CompileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String library;

    private String sourceFile;

    private String member;

    private String command;

    private String status;

    private String message;

    private String operator;

    private LocalDateTime createdTime;
}
