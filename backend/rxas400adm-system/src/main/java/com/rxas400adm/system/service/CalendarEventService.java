package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.dto.CalendarEventDTO;
import com.rxas400adm.system.entity.CalendarEvent;
import com.rxas400adm.system.mapper.CalendarEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 日历事件（rx_calendar_event）：按创建人隔离，月/范围/今日查询 + CRUD。
 * 参照旧项目 CalendarEventService。
 */
@Service
@RequiredArgsConstructor
public class CalendarEventService implements ICalendarEventService {

    private final CalendarEventMapper eventMapper;

    /** 按月查询（当月第一天 ~ 最后一天） */
    public List<CalendarEvent> month(int year, int month, Long userId) {
        YearMonth ym = YearMonth.of(year, month);
        return eventMapper.selectList(baseWrapper(userId)
                .ge(CalendarEvent::getEventDate, ym.atDay(1))
                .le(CalendarEvent::getEventDate, ym.atEndOfMonth())
                .orderByAsc(CalendarEvent::getEventDate)
                .orderByAsc(CalendarEvent::getStartTime));
    }

    /** 按日期范围查询 */
    public List<CalendarEvent> range(String startDate, String endDate, Long userId) {
        return eventMapper.selectList(baseWrapper(userId)
                .ge(CalendarEvent::getEventDate, LocalDate.parse(startDate))
                .le(CalendarEvent::getEventDate, LocalDate.parse(endDate))
                .orderByAsc(CalendarEvent::getEventDate)
                .orderByAsc(CalendarEvent::getStartTime));
    }

    /** 今日事件 */
    public List<CalendarEvent> today(Long userId) {
        return eventMapper.selectList(baseWrapper(userId)
                .eq(CalendarEvent::getEventDate, LocalDate.now())
                .orderByAsc(CalendarEvent::getStartTime));
    }

    
    public CalendarEvent create(CalendarEventDTO dto, Long userId) {
        CalendarEvent event = new CalendarEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setEventType(dto.getEventType());
        event.setPriority(dto.getPriority());
        event.setColor(dto.getColor());
        event.setIsAllDay(dto.getIsAllDay());
        event.setStatus(dto.getStatus());
        if (!StringUtils.hasText(event.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "事件标题必填");
        }
        if (event.getEventDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "事件日期必填");
        }
        event.setId(null);
        event.setUserId(userId);
        event.setEventType(event.getEventType() == null ? "other" : event.getEventType());
        event.setPriority(event.getPriority() == null ? 1 : event.getPriority());
        event.setIsAllDay(event.getIsAllDay() == null ? 1 : event.getIsAllDay());
        event.setStatus(event.getStatus() == null ? 1 : event.getStatus());
        event.setCreatedTime(LocalDateTime.now());
        event.setUpdatedTime(LocalDateTime.now());
        eventMapper.insert(event);
        return event;
    }

    
    public CalendarEvent update(Long id, CalendarEventDTO dto, Long userId) {
        CalendarEvent event = requireOwned(id, userId);
        if (StringUtils.hasText(dto.getTitle())) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getStartTime() != null) event.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) event.setEndTime(dto.getEndTime());
        if (StringUtils.hasText(dto.getEventType())) event.setEventType(dto.getEventType());
        if (dto.getPriority() != null) event.setPriority(dto.getPriority());
        if (dto.getColor() != null) event.setColor(dto.getColor());
        if (dto.getIsAllDay() != null) event.setIsAllDay(dto.getIsAllDay());
        if (dto.getStatus() != null) event.setStatus(dto.getStatus());
        event.setUpdatedTime(LocalDateTime.now());
        eventMapper.updateById(event);
        return event;
    }

    
    public void delete(Long id, Long userId) {
        requireOwned(id, userId);
        eventMapper.deleteById(id);
    }

    private LambdaQueryWrapper<CalendarEvent> baseWrapper(Long userId) {
        return new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getUserId, userId)
                .eq(CalendarEvent::getStatus, 1);
    }

    /** 校验事件存在且归属当前用户 */
    private CalendarEvent requireOwned(Long id, Long userId) {
        CalendarEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日历事件不存在");
        }
        if (!event.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人的日历事件");
        }
        return event;
    }
}