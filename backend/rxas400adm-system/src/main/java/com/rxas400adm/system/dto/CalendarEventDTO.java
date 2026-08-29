package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "事件标题", example = "团队会议")
    private String title;

    @Schema(description = "事件描述", example = "周例会")
    private String description;

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "事件日期", example = "2025-01-01")
    private LocalDate eventDate;

    @Schema(description = "开始时间", example = "09:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", example = "10:00")
    private LocalTime endTime;

    @Schema(description = "事件类型", example = "MEETING")
    private String eventType;

    @Schema(description = "优先级", example = "1")
    private Integer priority;

    @Schema(description = "颜色", example = "#1890ff")
    private String color;

    @Schema(description = "是否全天", example = "0")
    private Integer isAllDay;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;
}