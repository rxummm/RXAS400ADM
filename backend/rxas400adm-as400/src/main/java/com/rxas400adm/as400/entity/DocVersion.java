package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本历史（rx_doc_version，3.9）：每次保存/更新留档快照，可回溯。
 */
@Data
@TableName("rx_doc_version")
public class DocVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;

    private Integer version;

    private String title;

    private String content;

    private String operator;

    private LocalDateTime createdTime;
}
