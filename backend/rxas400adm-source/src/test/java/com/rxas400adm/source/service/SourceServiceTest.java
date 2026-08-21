package com.rxas400adm.source.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;
    @Mock
    private AS400Client client;

    private SourceService service;

    @BeforeEach
    void setUp() {
        service = new SourceService(clientProvider);
        lenient().when(clientProvider.current()).thenReturn(client);
    }

    @Test
    @DisplayName("列出库 → 委托 client")
    void listLibraries_shouldDelegate() {
        when(client.listLibraries()).thenReturn(List.of("MYLIB", "QSYS2"));
        List<String> libs = service.listLibraries();
        assertEquals(2, libs.size());
        verify(client).listLibraries();
    }

    @Test
    @DisplayName("列出源文件 → 委托 client")
    void listSourceFiles_shouldDelegate() {
        when(client.listSourceFiles("MYLIB")).thenReturn(List.of("QRPGLESRC"));
        List<String> files = service.listSourceFiles("MYLIB");
        assertEquals(1, files.size());
        verify(client).listSourceFiles("MYLIB");
    }

    @Test
    @DisplayName("列出成员 → 委托 client")
    void listMembers_shouldDelegate() {
        when(client.listMembers("MYLIB", "QRPGLESRC")).thenReturn(List.of("ORDERMAINT"));
        List<String> members = service.listMembers("MYLIB", "QRPGLESRC");
        assertEquals(1, members.size());
    }

    @Test
    @DisplayName("读取成员 → 返回结构化 Map")
    void readMember_shouldReturnStructuredMap() {
        when(client.readMember("MYLIB", "QRPGLESRC", "ORDERMAINT")).thenReturn("D CUSTNO S 7P 0");
        Map<String, String> result = service.readMember("MYLIB", "QRPGLESRC", "ORDERMAINT");
        assertEquals("MYLIB", result.get("library"));
        assertEquals("QRPGLESRC", result.get("sourceFile"));
        assertEquals("ORDERMAINT", result.get("member"));
        assertEquals("D CUSTNO S 7P 0", result.get("content"));
    }
}
