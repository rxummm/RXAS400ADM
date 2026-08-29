package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.GraphData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobDependencyService 测试")
class JobDependencyServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;
    @Mock
    private AS400Client as400Client;

    private JobDependencyService service;

    @BeforeEach
    void setUp() {
        service = new JobDependencyService(clientProvider);
    }

    @Test
    @DisplayName("graph() — 委托客户端返回依赖图")
    void graph_delegatesToClient() {
        GraphData mockData = GraphData.empty();
        when(clientProvider.current()).thenReturn(as400Client);
        when(as400Client.jobDependencies()).thenReturn(mockData);

        GraphData result = service.graph();

        assertNotNull(result);
        verify(clientProvider).current();
        verify(as400Client).jobDependencies();
    }
}
