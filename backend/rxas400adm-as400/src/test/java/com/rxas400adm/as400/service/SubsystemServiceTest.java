package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.SubsystemRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubsystemServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private SubsystemService service;

    @BeforeEach
    void setUp() {
        service = new SubsystemService(clientProvider);
        when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void list_shouldReturnSubsystems() {
        when(client.listSubsystems()).thenReturn(List.of(
                new SubsystemRow("QINTER", "交互作业子系统", "ACTIVE", 5L, 20L, "QSYS")
        ));
        assertEquals(1, service.list().size());
        assertEquals("QINTER", service.list().get(0).name());
        assertEquals("ACTIVE", service.list().get(0).status());
    }

    @Test
    void start_shouldDelegate() {
        when(client.startSubsystem("QBATCH")).thenReturn(CommandResult.ok("started"));
        CommandResult result = service.start("QBATCH");
        assertTrue(result.success());
        verify(client).startSubsystem("QBATCH");
    }

    @Test
    void end_shouldDelegate() {
        when(client.endSubsystem("QINTER")).thenReturn(CommandResult.ok("ended"));
        assertTrue(service.end("QINTER").success());
        verify(client).endSubsystem("QINTER");
    }
}
