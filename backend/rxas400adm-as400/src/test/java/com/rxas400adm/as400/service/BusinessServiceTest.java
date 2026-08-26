package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private BusinessService service;

    @BeforeEach
    void setUp() {
        service = new BusinessService(clientProvider);
    }

    @Test
    void columns_shouldRejectLibraryWithSemicolon() {
        // S8：库名含分号（SQL 多语句注入向量），必须在触达 client 前被白名单拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.columns("LIB;DROP", "USRFIL"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verifyNoInteractions(client);
    }

    @Test
    void columns_shouldRejectFileWithSpace() {
        // S8：文件名含空格（不在 A-Z 0-9 _ $ # @ 白名单内），直接拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.columns("MYLIB", "ORDER FILE"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verifyNoInteractions(client);
    }
}
