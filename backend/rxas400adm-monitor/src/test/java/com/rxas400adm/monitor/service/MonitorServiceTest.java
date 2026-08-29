package com.rxas400adm.monitor.service;

import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.monitor.vo.CompareResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private IIbmiSystemService ibmiSystemService;
    @Mock
    private IMetricService metricService;

    private MonitorService service;

    @BeforeEach
    void setUp() {
        // 使用 CallerRunsPolicy 线程池，避免测试中启动线程
        service = new MonitorService(ibmiSystemService, metricService, Executors.newSingleThreadExecutor());
    }

    @Test
    @DisplayName("compare → 空 ID 列表返回空")
    void compare_emptyIds_shouldReturnEmpty() {
        assertTrue(service.compare(List.of()).isEmpty());
        assertTrue(service.compare(null).isEmpty());
    }

    @Test
    @DisplayName("compare → 单台服务器对比")
    void compare_singleServer_shouldReturn() {
        IbmiSystem sys = new IbmiSystem();
        sys.setId(1L);
        sys.setName("PROD400");
        sys.setHost("10.0.0.1");
        sys.setEnvironment("PROD");
        sys.setStatus("ONLINE");
        when(ibmiSystemService.get(1L)).thenReturn(sys);
        when(metricService.overview(1L)).thenReturn(Map.of("CPU", 95.0));

        List<CompareResultVO> result = service.compare(List.of(1L));
        assertEquals(1, result.size());
        assertEquals("PROD400", result.get(0).name());
        assertEquals(95.0, result.get(0).metrics().get("CPU"));
    }

    @Test
    @DisplayName("compare → 多台服务器按 ID 顺序排列")
    void compare_multipleServers_shouldPreserveOrder() {
        IbmiSystem sys1 = new IbmiSystem();
        sys1.setId(1L);
        sys1.setName("PROD400");
        IbmiSystem sys2 = new IbmiSystem();
        sys2.setId(2L);
        sys2.setName("TEST400");
        when(ibmiSystemService.get(1L)).thenReturn(sys1);
        when(ibmiSystemService.get(2L)).thenReturn(sys2);
        when(metricService.overview(anyLong())).thenReturn(Map.of());

        List<CompareResultVO> result = service.compare(List.of(2L, 1L));
        assertEquals(2, result.size());
        assertEquals("TEST400", result.get(0).name());
        assertEquals("PROD400", result.get(1).name());
    }

    @Test
    @DisplayName("compare → 单台异常降级为占位行")
    void compare_singleFails_shouldFallback() {
        when(ibmiSystemService.get(1L)).thenThrow(new RuntimeException("连接失败"));

        List<CompareResultVO> result = service.compare(List.of(1L));
        assertEquals(1, result.size());
        assertEquals("SERVER-1", result.get(0).name());
        assertTrue(result.get(0).metrics().isEmpty());
    }

    @Test
    @DisplayName("compare → 最多 20 台（截断）")
    void compare_tooMany_shouldTruncate() {
        List<Long> ids = LongStream.rangeClosed(1, 25).boxed().toList();
        // 不需要 mock 每台，空列表就够验证截断
        when(ibmiSystemService.get(anyLong())).thenThrow(new RuntimeException());
        List<CompareResultVO> result = service.compare(ids);
        assertEquals(20, result.size());
    }
}
