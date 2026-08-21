package com.rxas400adm.compile.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.compile.dto.CompileRequest;
import com.rxas400adm.compile.entity.CompileRecord;
import com.rxas400adm.compile.mapper.CompileRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompileServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;
    @Mock
    private AS400Client client;
    @Mock
    private CompileRecordMapper recordMapper;

    private CompileService service;

    @BeforeEach
    void setUp() {
        service = new CompileService(clientProvider, recordMapper);
        lenient().when(clientProvider.current()).thenReturn(client);
    }

    @Test
    @DisplayName("编译成功 → 记录状态 SUCCESS")
    void compile_success_shouldRecordSuccess() {
        when(client.execute(anyString())).thenReturn(CommandResult.ok("编译成功"));
        when(recordMapper.insert(any(CompileRecord.class))).thenReturn(1);

        CompileRequest req = new CompileRequest();
        req.setLibrary("MYLIB");
        req.setSourceFile("QRPGLESRC");
        req.setMember("ORDERMAINT");
        req.setCommand("CRTBNDRPG");

        CompileRecord record = service.compile(req);

        assertEquals("SUCCESS", record.getStatus());
        assertEquals("MYLIB", record.getLibrary());
        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).execute(cmdCaptor.capture());
        assertTrue(cmdCaptor.getValue().contains("CRTBNDRPG"));
        assertTrue(cmdCaptor.getValue().contains("MYLIB"));
        verify(recordMapper).insert(any(CompileRecord.class));
    }

    @Test
    @DisplayName("编译失败 → 记录状态 FAILED")
    void compile_failure_shouldRecordFailed() {
        when(client.execute(anyString())).thenReturn(CommandResult.fail("RNF0107 语法错误"));
        when(recordMapper.insert(any(CompileRecord.class))).thenReturn(1);

        CompileRequest req = new CompileRequest();
        req.setLibrary("MYLIB");
        req.setSourceFile("QRPGLESRC");
        req.setMember("ORDERMAINT");

        CompileRecord record = service.compile(req);

        assertEquals("FAILED", record.getStatus());
        assertTrue(record.getMessage().contains("RNF0107"));
    }

    @Test
    @DisplayName("不支持的编译命令 → 抛异常")
    void compile_unsupportedCommand_shouldThrow() {
        CompileRequest req = new CompileRequest();
        req.setLibrary("MYLIB");
        req.setSourceFile("QRPGLESRC");
        req.setMember("TEST");
        req.setCommand("DLTLIB");

        assertThrows(BusinessException.class, () -> service.compile(req));
    }

    @Test
    @DisplayName("非法标识符（含空格/引号）→ 抛异常")
    void compile_invalidIdentifier_shouldThrow() {
        CompileRequest req = new CompileRequest();
        req.setLibrary("MY LIB");
        req.setSourceFile("QRPGLESRC");
        req.setMember("TEST");

        assertThrows(BusinessException.class, () -> service.compile(req));
    }

    @Test
    @DisplayName("历史记录查询 → 委托 Mapper")
    void history_shouldDelegateToMapper() {
        when(recordMapper.selectList(null)).thenReturn(List.of(new CompileRecord()));
        assertEquals(1, service.history().size());
        verify(recordMapper).selectList(null);
    }
}
