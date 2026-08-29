package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档（rx_sys_doc，V66）：纯 DB 元数据，无 IFS 存储。
 */
@Data
@TableName("rx_sys_doc")
public class SysDoc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 分类 */
    private String category;

    /** 标签（逗号分隔） */
    private String tags;

    /** DRAFT / PUBLISHED */
    private String status;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
