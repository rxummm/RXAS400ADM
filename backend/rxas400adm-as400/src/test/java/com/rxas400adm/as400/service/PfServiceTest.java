package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PfServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private PfService service;

    @BeforeEach
    void setUp() {
        service = new PfService(clientProvider);
        when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void files_shouldReturnPfList() {
        when(client.listPfFiles("APP")).thenReturn(List.of(
                new PfRow("CUSTMAST", "APP", "客户主档")
        ));
        List<PfRow> rows = service.files("APP");
        assertEquals(1, rows.size());
        assertEquals("CUSTMAST", rows.get(0).tableName());
    }

    @Test
    void columns_shouldReturnColumns() {
        when(client.pfColumns("APP", "CUSTMAST")).thenReturn(List.of(
                new PfColumnRow("CUSTID", "DECIMAL", 15, "N")
        ));
        List<PfColumnRow> rows = service.columns("APP", "CUSTMAST");
        assertEquals("DECIMAL", rows.get(0).type());
    }

    @Test
    void data_shouldReturnRowsWithLimit() {
        when(client.pfData("APP", "CUSTMAST", 20)).thenReturn(List.of(
                Map.of("CUSTID", "1001", "CUSTNAME", "IBM")
        ));
        List<Map<String, Object>> rows = service.data("APP", "CUSTMAST", 20);
        assertEquals("1001", rows.get(0).get("CUSTID"));
    }
}
