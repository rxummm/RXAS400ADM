package com.rxas400adm.system.service;

import com.rxas400adm.system.dto.CalendarEventDTO;
import com.rxas400adm.system.entity.CalendarEvent;

import java.util.List;

/**
 * 日历事件服务接口（rx_calendar_event）。
 */
public interface ICalendarEventService {

    List<CalendarEvent> month(int year, int month, Long userId);

    List<CalendarEvent> range(String startDate, String endDate, Long userId);

    List<CalendarEvent> today(Long userId);

    CalendarEvent create(CalendarEventDTO event, Long userId);

    CalendarEvent update(Long id, CalendarEventDTO dto, Long userId);

    void delete(Long id, Long userId);
}
