package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告（rx_notice）：管理端发布的公告，发布时向全体活跃用户写入站内通知。
 */
@Data
@TableName("rx_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 1=发布 0=下架 */
    private Integer status;

    private LocalDateTime publishedTime;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}