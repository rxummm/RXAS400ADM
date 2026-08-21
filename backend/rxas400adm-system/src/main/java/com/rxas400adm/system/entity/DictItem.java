package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典项（rx_dict_item）。
 */
@Data
@TableName("rx_dict_item")
public class DictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;

    private String itemKey;

    private String itemValue;

    private Integer sort;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
