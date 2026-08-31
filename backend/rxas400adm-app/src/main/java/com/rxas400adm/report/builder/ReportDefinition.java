package com.rxas400adm.report.builder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自定义报表定义（rx_report_definition）：存储用户配置的字段/筛选/排序。
 * columns_json / filters_json / sorts_json 为 JSON 字符串，前端解析。
 */
@Data
@TableName("rx_report_definition")
public class ReportDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 数据源标识：orders/inventory/items/... */
    private String dataSource;

    private String title;

    /** 选中列 JSON [{key, label, type}] */
    private String columnsJson;

    /** 筛选条件 JSON [{field, op, value}] */
    private String filtersJson;

    /** 排序规则 JSON [{field, asc}] */
    private String sortsJson;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
