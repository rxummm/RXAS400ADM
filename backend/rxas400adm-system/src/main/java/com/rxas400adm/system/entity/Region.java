package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 中国行政区划（rx_region）：省/市/区县三级，参照旧项目 china_regions。
 */
@Data
@TableName("rx_region")
public class Region {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 行政区划代码（6 位） */
    private String code;

    private String name;

    /** 1=省 2=市 3=区/县 */
    private Integer level;

    /** 上级代码（空=省） */
    private String parentCode;

    private String pinyin;

    private String abbreviation;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
