package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsOrderQueryDTO;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsOrderHeaderVO;
import com.rxas400adm.as400.vo.BpcsOrderLineVO;
import com.rxas400adm.common.config.ProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

/**
 * 【第六章·P1】BPCS 订单时间轴服务测试：
 * 状态推导纯函数 + mock 演示数据链路 + XML 语句注册器集成。
 */
@ExtendWith(MockitoExtension.class)
class BpcsOrderServiceImplTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private ProfileResolver profileResolver;

    private BpcsOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        // bpcs.library 固定 BPCSF；ProfileResolver 默认非 mock，用例内按需覆盖
        service = new BpcsOrderServiceImpl(clientProvider, profileResolver,
                new SqlStatementRegistry(), (k, d) -> "BPCSF");
    }

    // ---------------- 状态推导纯函数 ----------------

    @Test
    @DisplayName("阶段推导：全 0 → 录入(0)")
    void derive_allZero_stage0() {
        assertEquals(0, BpcsOrderServiceImpl.deriveStageIndex(false, false, false, false));
    }

    @Test
    @DisplayName("阶段推导：拣货释放 → 1")
    void derive_pickReleased_stage1() {
        assertEquals(1, BpcsOrderServiceImpl.deriveStageIndex(true, false, false, false));
    }

    @Test
    @DisplayName("阶段推导：拣货确认 → 2")
    void derive_pickConfirmed_stage2() {
        assertEquals(2, BpcsOrderServiceImpl.deriveStageIndex(true, true, false, false));
    }

    @Test
    @DisplayName("阶段推导：开票 → 3")
    void derive_billed_stage3() {
        assertEquals(3, BpcsOrderServiceImpl.deriveStageIndex(true, true, true, false));
    }

    @Test
    @DisplayName("阶段推导：关闭归档最高优先 → 4")
    void derive_closed_stage4() {
        assertEquals(4, BpcsOrderServiceImpl.deriveStageIndex(false, false, false, true));
    }

    // ---------------- mock 链路（演示数据 + 内存过滤） ----------------

    private void stubMockMode(boolean mock) {
        lenient().when(profileResolver.isMockMode()).thenReturn(mock);
    }

    @Test
    @DisplayName("mock：按订单号精确过滤演示行，行状态随 CLSTS 推导")
    void mockLines_filteredByOrno() {
        stubMockMode(true);
        BpcsOrderQueryDTO q = new BpcsOrderQueryDTO();
        q.setCono("001");
        q.setOrno("234567");

        List<BpcsOrderLineVO> lines = service.getLines(q);

        assertEquals(2, lines.size());
        // 行 001 已拣货确认 → stage 2
        assertEquals(2, lines.get(0).stageIndex());
        assertEquals("PICK_CONFIRMED", lines.get(0).stageKey());
        // 行 002 未释放 → stage 0，且分配/发运/开票数量为 null（未产生）
        assertEquals(0, lines.get(1).stageIndex());
        assertEquals(null, lines.get(1).qtyAllocated());
        assertEquals(null, lines.get(1).qtyShipped());
    }

    @Test
    @DisplayName("mock：未知订单号 → 空列表而非异常")
    void mockLines_unknownOrno_empty() {
        stubMockMode(true);
        BpcsOrderQueryDTO q = new BpcsOrderQueryDTO();
        q.setCono("001");
        q.setOrno("999999");

        assertEquals(List.of(), service.getLines(q));
    }

    @Test
    @DisplayName("mock：订单头时间轴节点裁剪（无发运两节点）+ 当前阶段")
    void mockHeader_timelineNodes() {
        stubMockMode(true);
        BpcsOrderQueryDTO q = new BpcsOrderQueryDTO();
        q.setCono("001");
        q.setOrno("234567");

        BpcsOrderHeaderVO header = service.getHeader(q);

        // 时间轴固定 5 节点：CREATED/PICK_RELEASED/PICK_CONFIRMED/BILLED/CLOSED
        assertEquals(5, header.timeline().size());
        assertEquals(2, header.currentStageIndex());
        assertEquals(Boolean.TRUE, header.timeline().get(0).reached());
        assertEquals(Boolean.TRUE, header.timeline().get(1).reached());
        assertEquals(Boolean.TRUE, header.timeline().get(2).reached());
        assertEquals(Boolean.FALSE, header.timeline().get(3).reached());
        // raw 五位透传完整（含恒 0 的 CHSTS3/4）
        assertEquals("11000", header.raw().chsts());
    }

    @Test
    @DisplayName("mock：已关闭订单 → CLOSED 节点达成且 HID=CZ")
    void mockHeader_closedOrder() {
        stubMockMode(true);
        BpcsOrderQueryDTO q = new BpcsOrderQueryDTO();
        q.setCono("001");
        q.setOrno("123456");

        BpcsOrderHeaderVO header = service.getHeader(q);

        assertEquals(4, header.currentStageIndex());
        assertEquals("CZ", header.raw().hid());
        assertEquals(Boolean.TRUE, header.timeline().get(4).reached());
    }
}