package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 日历事件（rx_calendar_event，参照旧项目 sys_calendar_event）：
 * 按创建人隔离，月/范围/今日查询 + 事件 CRUD。
 */
@Data
@TableName("rx_calendar_event")
public class CalendarEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建人（rx_user.id） */
    private Long userId;

    private String title;

    private String description;

    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    /** meeting/task/reminder/other */
    private String eventType;

    /** 1=低 2=中 3=高 */
    private Integer priority;

    /** 标记色（十六进制） */
    private String color;

    /** 1=全天 0=按时间 */
    private Integer isAllDay;

    /** 1=正常 0=已取消 */
    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
