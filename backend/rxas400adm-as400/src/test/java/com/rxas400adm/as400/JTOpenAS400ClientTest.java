package com.rxas400adm.as400;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JTOpenAS400ClientTest {

    @Spy
    private JTOpenAS400Client client = new JTOpenAS400Client("test-host", "test-user", "test-pass");

    private HikariDataSource mockPool;

    @BeforeEach
    void setUp() {
        // P1 连接池化后：查询走 state.pooledDataSource 池连接，预置 mock 池避免真实建连
        mockPool = mock(HikariDataSource.class);
        Object state = ReflectionTestUtils.getField(client, "state");
        ReflectionTestUtils.setField(state, "pooledDataSource", mockPool);
    }

    private void poolGetConnectionThrows(SQLException ex) throws SQLException {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(ex);
        when(mockPool.getConnection()).thenReturn(conn);
    }

    @Test
    void queryListChecked_sqlSyntaxError_shouldThrowSqlFailed() throws Exception {
        SQLException ex = new SQLException("语法错误", "42S22", -104);
        poolGetConnectionThrows(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM BAD_TABLE"));
        assertEquals(ErrorCode.AS400_SQL_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_sqlPermissionError_shouldThrowSqlFailed() throws Exception {
        SQLException ex = new SQLException("权限不足", "42501", -551);
        poolGetConnectionThrows(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SENSITIVE_TABLE"));
        assertEquals(ErrorCode.AS400_SQL_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_connectionErrorBySqlState_shouldThrowConnectionFailed() throws Exception {
        SQLException ex = new SQLException("连接失败", "08001", -4499);
        when(mockPool.getConnection()).thenThrow(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertEquals(ErrorCode.AS400_CONNECTION_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_nonTransientConnectionException_shouldThrowConnectionFailed() throws Exception {
        SQLNonTransientConnectionException ex = new SQLNonTransientConnectionException("连接不可用");
        when(mockPool.getConnection()).thenThrow(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertEquals(ErrorCode.AS400_CONNECTION_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_transientConnectionException_shouldThrowConnectionFailed() throws Exception {
        SQLTransientConnectionException ex = new SQLTransientConnectionException("连接超时");
        when(mockPool.getConnection()).thenThrow(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertEquals(ErrorCode.AS400_CONNECTION_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_recoverableException_shouldThrowConnectionFailed() throws Exception {
        SQLRecoverableException ex = new SQLRecoverableException("可恢复错误");
        when(mockPool.getConnection()).thenThrow(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertEquals(ErrorCode.AS400_CONNECTION_FAILED.getCode(), thrown.getCode());
    }

    @Test
    void queryListChecked_runtimeException_shouldThrowSqlFailed() throws Exception {
        RuntimeException ex = new RuntimeException("运行时异常");
        when(mockPool.getConnection()).thenThrow(ex);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> client.queryListChecked("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO"));
        assertEquals(ErrorCode.AS400_SQL_FAILED.getCode(), thrown.getCode());
    }
}