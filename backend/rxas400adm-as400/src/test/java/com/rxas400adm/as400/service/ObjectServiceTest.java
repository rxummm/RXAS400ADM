package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private ObjectService service;

    @BeforeEach
    void setUp() {
        service = new ObjectService(clientProvider);
        when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void searchObjects_shouldDelegateToCurrentServerClient() {
        when(client.searchObjects("APP", "PGM")).thenReturn(List.of(
                new ObjectRow("ORDERMAINT", "PGM", "APP", 1024L, "2026-03-12 09:00:00")
        ));
        var result = service.searchObjects("APP", "PGM", null, 1, 20);
        assertEquals(1, result.getTotal());
        assertEquals("ORDERMAINT", result.getRecords().get(0).name());
        assertEquals("APP", result.getRecords().get(0).library());
    }

    @Test
    void searchObjects_noType_shouldDelegateWithNull() {
        when(client.searchObjects("APP", null)).thenReturn(List.of());
        assertEquals(0, service.searchObjects("APP", null, null, 1, 20).getTotal());
    }

    @Test
    void searchObjects_shouldFilterKeywordAndPaginate() {
        when(client.searchObjects("APP", null)).thenReturn(List.of(
                new ObjectRow("ORDERMAINT", "PGM", "APP", 1L, null),
                new ObjectRow("CUSTMAINT", "PGM", "APP", 1L, null),
                new ObjectRow("PAYROLL", "PGM", "APP", 1L, null)
        ));
        // 关键字过滤：只命中 ORDERMAINT / CUSTMAINT
        var filtered = service.searchObjects("APP", null, "MAINT", 1, 20);
        assertEquals(2, filtered.getTotal());
        assertEquals("ORDERMAINT", filtered.getRecords().get(0).name());
        // 分页：size=1 时第二页仅一条（CUSTMAINT）
        var paged = service.searchObjects("APP", null, null, 2, 1);
        assertEquals(3, paged.getTotal());
        assertEquals(1, paged.getRecords().size());
        assertEquals("CUSTMAINT", paged.getRecords().get(0).name());
    }

    @Test
    void objectDetail_shouldDelegateToCurrentServerClient() {
        when(client.objectDetail("APP", "ORDERMAINT")).thenReturn(
                new ObjectDetail("ORDERMAINT", "PGM", "APP", 1L, null, null, null, "QSECOFR", null));
        ObjectDetail detail = service.objectDetail("APP", "ORDERMAINT");
        assertEquals("ORDERMAINT", detail.name());
        assertEquals("QSECOFR", detail.owner());
    }

    @Test
    void objectReferences_shouldPassDirection() {
        when(client.objectReferences("APP", "ORDERMAINT", "IN")).thenReturn(List.of(
                new ObjectRefRow("APP", "DAILYBAL", "PGM", "APP", "ORDERMAINT", "*PGM")
        ));
        when(client.objectReferences("APP", "ORDERMAINT", "OUT")).thenReturn(List.of(
                new ObjectRefRow("APP", "ORDERMAINT", "PGM", "APP", "INVCTL", "PGM")
        ));
        assertEquals(1, service.objectReferences("APP", "ORDERMAINT", "IN").size());
        assertEquals("INVCTL", service.objectReferences("APP", "ORDERMAINT", "OUT").get(0).refName());
    }
}
