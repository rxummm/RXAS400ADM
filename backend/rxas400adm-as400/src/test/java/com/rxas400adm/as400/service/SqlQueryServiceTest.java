package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.SqlHistory;
import com.rxas400adm.as400.mapper.SqlHistoryMapper;
import com.rxas400adm.as400.vo.QueryResult;
import com.rxas400adm.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlQueryServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private SqlHistoryMapper historyMapper;

    private SqlQueryService service;

    @BeforeEach
    void setUp() {
        service = new SqlQueryService(clientProvider, historyMapper);
        lenient().when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void execute_shouldReturnColumnsAndRows() {
        when(client.queryListCheckedBounded(any(String.class), anyInt())).thenReturn(List.of(
                Map.of("JOB_NAME", "JOB1", "JOB_STATUS", "RUN")
        ));
        QueryResult result = service.execute("SELECT JOB_NAME, JOB_STATUS FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X");
        assertTrue(result.getColumns().containsAll(List.of("JOB_NAME", "JOB_STATUS")));
        assertEquals(1, result.getRowsReturned());
        verify(historyMapper).insert(any(SqlHistory.class));
    }

    @Test
    void execute_nonSelect_shouldReject() {
        assertThrows(BusinessException.class, () -> service.execute("DELETE FROM QSYS2.SOMETHING"));
        assertThrows(BusinessException.class, () -> service.execute("UPDATE QSYS2.SOMETHING SET X=1"));
    }

    @Test
    void execute_qcmdexcTableFunction_shouldReject() {
        assertThrows(BusinessException.class,
                () -> service.execute("SELECT * FROM TABLE(QSYS2.QCMDEXC('CHGUSRPRF USRPRF(USER1) PASSWORD(X)')) X"));
        assertThrows(BusinessException.class,
                () -> service.execute("SELECT QSYS2.QCMDEXC('DSPLIB') FROM SYSIBM.SYSDUMMY1"));
        assertThrows(BusinessException.class,
                () -> service.execute("SELECT * FROM QSYS2.QSYSTEMS('CMD')"));
        assertThrows(BusinessException.class,
                () -> service.execute("SELECT * FROM TABLE(QSYS2.IFS_WRITE('/tmp/x', '*TEXT')) X"));
    }

    @Test
    void execute_readOnlyTableFunctions_shouldPass() {
        when(client.queryListCheckedBounded(any(String.class), anyInt())).thenReturn(List.of(Map.of("JOB_NAME", "JOB1")));
        QueryResult result = service.execute("SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X");
        assertEquals(1, result.getRowsReturned());
        result = service.execute("SELECT * FROM TABLE(QSYS2.JOB_LOG_INFO('*ALL', '*ALL', '*ALL', '*ALL')) X");
        assertEquals(1, result.getRowsReturned());
    }

    @Test
    void execute_largeResult_shouldLimitRows() {
        java.util.List<Map<String, Object>> many = new java.util.ArrayList<>();
        for (int i = 0; i < 300; i++) {
            many.add(Map.of("COL", "v" + i));
        }
        when(client.queryListCheckedBounded(any(String.class), anyInt())).thenReturn(many);
        QueryResult result = service.execute("SELECT COL FROM QSYS2.SOME_VIEW");
        assertEquals(300, result.getRowsReturned());
        assertTrue(result.getRows().size() <= 200);
    }
}
