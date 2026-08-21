package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.dto.CalendarEventDTO;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.service.ICalendarEventService;
import com.rxas400adm.system.service.SysUserService;
import com.rxas400adm.system.vo.CalendarEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 日历事件（rx_calendar_event）：按创建人隔离，月/范围/今日查询 + CRUD。
 * 权限码：CALENDAR_VIEW（查询）/ CALENDAR_MANAGE（写操作）。
 */
@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
@Tag(name = "日历管理")
public class CalendarController {

    private final ICalendarEventService eventService;
    private final SysUserService userService;

    @GetMapping("/month")
    @PreAuthorize("hasAuthority('CALENDAR_VIEW')")
    public ApiResponse<List<CalendarEventVO>> month(@RequestParam int year,
                                                  @RequestParam int month) {
        return ApiResponse.success(eventService.month(year, month, currentUserId()).stream().map(CalendarEventVO::from).toList());
    }

    @GetMapping("/range")
    @PreAuthorize("hasAuthority('CALENDAR_VIEW')")
    public ApiResponse<List<CalendarEventVO>> range(@RequestParam String startDate,
                                                  @RequestParam String endDate) {
        return ApiResponse.success(eventService.range(startDate, endDate, currentUserId()).stream().map(CalendarEventVO::from).toList());
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('CALENDAR_VIEW')")
    public ApiResponse<List<CalendarEventVO>> today() {
        return ApiResponse.success(eventService.today(currentUserId()).stream().map(CalendarEventVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CALENDAR_MANAGE')")
    @OperateLog(module = "日历", operation = "新增事件")
    public ApiResponse<CalendarEventVO> create(@Valid @RequestBody CalendarEventDTO event) {
        return ApiResponse.success(CalendarEventVO.from(eventService.create(event, currentUserId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CALENDAR_MANAGE')")
    @OperateLog(module = "日历", operation = "修改事件")
    public ApiResponse<CalendarEventVO> update(@PathVariable Long id, @Valid @RequestBody CalendarEventDTO dto) {
        return ApiResponse.success(CalendarEventVO.from(eventService.update(id, dto, currentUserId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CALENDAR_MANAGE')")
    @OperateLog(module = "日历", operation = "删除事件")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.delete(id, currentUserId());
        return ApiResponse.success(null);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? "anonymous" : authentication.getName();
        SysUser user = userService.getByUsername(username);
        return user == null ? -1L : user.getId();
    }
}