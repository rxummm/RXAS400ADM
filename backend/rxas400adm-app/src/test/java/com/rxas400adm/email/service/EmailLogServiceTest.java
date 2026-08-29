package com.rxas400adm.email.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.entity.EmailLog;
import com.rxas400adm.email.mapper.EmailLogMapper;
import com.rxas400adm.email.vo.EmailLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailLogService 测试")
class EmailLogServiceTest {

    @Mock
    private EmailLogMapper emailLogMapper;

    private EmailLogService service;

    @BeforeEach
    void setUp() {
        service = new EmailLogService(emailLogMapper);
    }

    @Test
    @DisplayName("page() — 返回分页结果")
    void page_normal() {
        EmailLog log = createSampleLog();
        Page<EmailLog> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(emailLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<EmailLogVO> result = service.page(1, 10, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("page() — 带 keyword 过滤")
    void page_withKeyword() {
        Page<EmailLog> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(emailLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<EmailLogVO> result = service.page(1, 10, null, null, "test");

        assertNotNull(result);
        verify(emailLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("detail() — 存在返回 VO")
    void detail_exists() {
        EmailLog log = createSampleLog();
        log.setId(1L);
        when(emailLogMapper.selectById(1L)).thenReturn(log);

        EmailLogVO result = service.detail(1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("detail() — 不存在返回 null")
    void detail_notExists() {
        when(emailLogMapper.selectById(99L)).thenReturn(null);

        EmailLogVO result = service.detail(99L);

        assertNull(result);
    }

    private EmailLog createSampleLog() {
        EmailLog log = new EmailLog();
        log.setId(1L);
        log.setSubject("Test Subject");
        log.setRecipients("a@test.com");
        log.setChannel("ALERT");
        log.setStatus("SENT");
        log.setCreatedTime(LocalDateTime.now());
        return log;
    }
}
