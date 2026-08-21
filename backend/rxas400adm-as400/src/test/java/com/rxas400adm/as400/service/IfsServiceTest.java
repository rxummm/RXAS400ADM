package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.IfsEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IfsServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private IfsService service;

    @BeforeEach
    void setUp() {
        service = new IfsService(clientProvider);
        when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void list_shouldReturnDirEntries() {
        when(client.listIfsDir("/QOpenSys/rxas400")).thenReturn(List.of(
                new IfsEntry("document", "DIR", 0L, "2026-07-10 10:00:00", "/QOpenSys/rxas400/document")
        ));
        List<IfsEntry> rows = service.list("/QOpenSys/rxas400");
        assertEquals(1, rows.size());
        assertEquals("DIR", rows.get(0).type());
        assertEquals("document", rows.get(0).name());
    }

    @Test
    void read_shouldReturnFileContent() {
        when(client.readIfsFile("/QOpenSys/rxas400/README.txt")).thenReturn("content");
        assertEquals("content", service.read("/QOpenSys/rxas400/README.txt"));
    }
}
