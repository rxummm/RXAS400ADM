package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * QSYS2 SQL 执行历史（rx_sql_history）
 */
@Data
@TableName("rx_sql_history")
public class SqlHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sqlText;

    private Integer rowsReturned;

    private Long costMs;

    private String operator;

    private LocalDateTime createdTime;
}
