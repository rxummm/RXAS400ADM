package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.CommandScriptRequest;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandScriptServiceTest {

    @Mock
    private CommandScriptMapper scriptMapper;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private CommandScriptService service;

    @BeforeEach
    void setUp() {
        service = new CommandScriptService(scriptMapper, clientProvider);
        org.mockito.Mockito.lenient().when(clientProvider.forServer(1L)).thenReturn(client);
    }

    private CommandScriptRequest request() {
        CommandScriptRequest req = new CommandScriptRequest();
        req.setName("查看作业");
        req.setCommand("WRKACTJOB");
        req.setTags("job,ops");
        req.setFavorite(true);
        return req;
    }

    @Test
    void create_shouldSetFavoriteAndDefaults() {
        when(scriptMapper.insert(any(CommandScript.class))).thenAnswer(inv -> {
            ((CommandScript) inv.getArgument(0)).setId(1L);
            return 1;
        });
        CommandScript created = service.create(request(), "admin");
        assertEquals(1L, created.getId());
        assertEquals("admin", created.getCreatedBy());
        assertEquals(0, created.getRunCount());
        verify(scriptMapper).insert(any(CommandScript.class));
    }

    @Test
    void toggleFavorite_shouldUpdate() {
        CommandScript script = new CommandScript();
        script.setId(5L);
        script.setFavorite(false);
        when(scriptMapper.selectById(5L)).thenReturn(script);
        CommandScript updated = service.toggleFavorite(5L, true);
        assertEquals(true, updated.getFavorite());
        verify(scriptMapper).updateById(script);
    }

    @Test
    void execute_shouldIncrementRunCountAndRecordResult() {
        CommandScript script = new CommandScript();
        script.setId(5L);
        script.setName("查看作业");
        script.setCommand("WRKACTJOB");
        script.setRunCount(2);
        when(scriptMapper.selectById(5L)).thenReturn(script);
        when(client.execute("WRKACTJOB")).thenReturn(CommandResult.ok("ok"));

        CommandResult result = service.execute(5L, 1L);

        assertEquals(true, result.success());
        assertEquals(3, script.getRunCount());
        verify(scriptMapper).updateById(script);
    }

    @Test
    void execute_withoutServer_shouldNotRun() {
        CommandScript script = new CommandScript();
        script.setId(5L);
        when(scriptMapper.selectById(5L)).thenReturn(script);
        try {
            service.execute(5L, null);
        } catch (Exception ignored) {
            // 期望业务异常
        }
        verify(client, never()).execute(any());
    }
}
