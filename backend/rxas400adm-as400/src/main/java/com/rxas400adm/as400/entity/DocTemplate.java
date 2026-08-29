package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档模板（rx_doc_template，3.9）。
 */
@Data
@TableName("rx_doc_template")
public class DocTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private String content;

    /** 模板类型：MARKDOWN / TEXT / HTML（应用模板时联动设置文档类型） */
    private String docType;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
