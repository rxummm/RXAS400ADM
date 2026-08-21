package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典类型（rx_dict_type）。
 */
@Data
@TableName("rx_dict_type")
public class DictType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String remark;

    private Integer sort;

    /** 1=启用 0=停用 */
    private Integer status;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
