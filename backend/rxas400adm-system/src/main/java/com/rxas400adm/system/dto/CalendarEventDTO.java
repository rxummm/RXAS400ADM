package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 日历事件写请求 DTO（create/update 共用）。
 * 不含 id/userId（归属由登录用户决定）/createdTime/updatedTime。
 */
@Data
public class CalendarEventDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String title;

    private String description;

    @NotNull(message = "{validation.notNull}")
    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String eventType;

    private Integer priority;

    private String color;

    private Integer isAllDay;

    private Integer status;
}
