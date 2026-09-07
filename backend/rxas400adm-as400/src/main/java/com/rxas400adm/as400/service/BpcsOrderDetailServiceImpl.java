package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsOrderLineDetailVO;
import com.rxas400adm.as400.vo.BpcsOrderTimelineEventVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * 订单详情增强服务实现（⑰）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsOrderDetailServiceImpl implements IBpcsOrderDetailService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsOrderLineDetailVO> getLineDetails(String cono, String orno) {
        if (profileResolver.isMockMode()) {
            return mockLineDetails();
        }
        String sql = statements.get("bpcs.order.lineDetail");
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono, orno);
        List<BpcsOrderLineDetailVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer qtyOrd = BpcsRowUtil.intOrNull(row, "QTORD");
            Integer qtyShip = BpcsRowUtil.intOrNull(row, "QTSHP");
            Integer qtyInv = BpcsRowUtil.intOrNull(row, "QTINV");
            String status = deriveStatusLabel(qtyOrd, qtyShip, qtyInv);
            result.add(new BpcsOrderLineDetailVO(
                    pickStr(row, "CONO"), pickStr(row, "ORNO"), pickStr(row, "ORLN"),
                    pickStr(row, "ITEM"), pickStr(row, "ITDSC"),
                    qtyOrd, BpcsRowUtil.intOrNull(row, "QTYALC"), qtyShip, qtyInv,
                    BpcsRowUtil.decOrNull(row, "PRICE"), BpcsRowUtil.decOrNull(row, "SHIP_AMOUNT"),
                    pickStr(row, "CUST"), pickStr(row, "REQDTE"), pickStr(row, "ORDTE"),
                    pickStr(row, "SHIPDTE"), pickStr(row, "SHSTAT"), pickStr(row, "LHNO"),
                    pickStr(row, "INVNO"), pickStr(row, "INVDTE"), BpcsRowUtil.decOrNull(row, "INVAMT"),
                    status
            ));
        }
        return result;
    }

    @Override
    public List<BpcsOrderTimelineEventVO> getTimeline(String cono, String orno) {
        if (profileResolver.isMockMode()) {
            return mockTimeline();
        }
        String sql = statements.get("bpcs.order.timeline");
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono, orno, cono, orno, cono, orno);
        List<BpcsOrderTimelineEventVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsOrderTimelineEventVO(
                    pickStr(row, "EVENT_TYPE"),
                    pickStr(row, "EVENT_DATE"),
                    pickStr(row, "EVENT_DESC")
            ));
        }
        return result;
    }

    private String deriveStatusLabel(Integer qtyOrd, Integer qtyShip, Integer qtyInv) {
        if (qtyOrd == null || qtyOrd == 0) return "PENDING";
        int shipped = qtyShip != null ? qtyShip : 0;
        int invoiced = qtyInv != null ? qtyInv : 0;
        if (invoiced >= qtyOrd) return "INVOICED";
        if (shipped >= qtyOrd) return "SHIPPED";
        if (shipped > 0) return "PARTIAL_SHIP";
        int allocated = 0; // would need QTYALC
        if (allocated > 0) return "ALLOCATED";
        return "ORDERED";
    }

    private List<BpcsOrderLineDetailVO> mockLineDetails() {
        return List.of(
                new BpcsOrderLineDetailVO("001", "123456", "001", "DEF-2001", "螺柱 M12x30", 500, 500, 500, 500, new BigDecimal("18.00"), new BigDecimal("9000.00"), "20315", "20250401", "20250312", "20250328", "1", "LH-001", "INV-2025001", "20250402", new BigDecimal("9000.00"), "INVOICED"),
                new BpcsOrderLineDetailVO("001", "123456", "003", "DEF-2003", "垫片 M12", 1000, 800, 600, 0, new BigDecimal("0.50"), new BigDecimal("300.00"), "20315", "20250401", "20250312", "20250328", "1", "LH-001", null, null, null, "PARTIAL_SHIP"),
                new BpcsOrderLineDetailVO("001", "234567", "001", "DEF-2005", "轴承 6205", 200, 100, 0, 0, new BigDecimal("45.00"), null, "20777", "20250815", "20250720", null, null, null, null, null, null, "ALLOCATED")
        );
    }

    private List<BpcsOrderTimelineEventVO> mockTimeline() {
        return List.of(
                new BpcsOrderTimelineEventVO("CREATED", "20250312", "Order Created"),
                new BpcsOrderTimelineEventVO("SHIPPED", "20250328", "Shipped via LH-001"),
                new BpcsOrderTimelineEventVO("INVOICED", "20250402", "Invoiced: INV-2025001")
        );
    }
}
