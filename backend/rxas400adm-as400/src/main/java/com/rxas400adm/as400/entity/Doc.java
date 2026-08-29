package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运维文档（rx_doc，3.9）：模板起草 + 版本管理 + 审批状态机。
 * 状态：DRAFT → PENDING（提交审批）→ PUBLISHED（通过）/ REJECTED（驳回）。
 */
@Data
@TableName("rx_doc")
public class Doc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String title;

    private String content;

    /** 文档类型：MARKDOWN / TEXT / HTML（正文渲染）/ PDF / IMAGE（IFS 文件渲染） */
    private String docType;

    private Integer version;

    /** DRAFT / PENDING / PUBLISHED / REJECTED */
    private String status;

    private String rejectReason;

    /** 最近一次上传到 IFS 的目标路径（文档管理「上传到 IFS」） */
    private String ifsPath;

    /** 逻辑删除标记：0=正常 1=已删除（删除文档=标记不物理删，保留审批/版本历史） */
    private Integer deleted;

    /** 逻辑删除时间（已删除视图展示用） */
    private LocalDateTime deletedTime;

    private String createdBy;

    private String updatedBy;

    private String approvedBy;

    private LocalDateTime approvedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    /** 模板名称（列表展示用，非表字段） */
    @TableField(exist = false)
    private String templateName;
}
