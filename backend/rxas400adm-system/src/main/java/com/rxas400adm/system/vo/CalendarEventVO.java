package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.CalendarEvent;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CalendarEventVO {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String eventType;
    private Integer priority;
    private String color;
    private Integer isAllDay;
    private Integer status;

    public static CalendarEventVO from(CalendarEvent entity) {
        CalendarEventVO vo = new CalendarEventVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setEventDate(entity.getEventDate());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setEventType(entity.getEventType());
        vo.setPriority(entity.getPriority());
        vo.setColor(entity.getColor());
        vo.setIsAllDay(entity.getIsAllDay());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}