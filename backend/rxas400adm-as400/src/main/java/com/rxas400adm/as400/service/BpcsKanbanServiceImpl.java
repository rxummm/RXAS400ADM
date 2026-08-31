package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsKanbanVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ⑯ 订单看板视图实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsKanbanServiceImpl implements IBpcsKanbanService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsKanbanVO> listKanbanOrders(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return List.of(
                    new BpcsKanbanVO("001", "MO-100234", "20315", "张三科技", "20260820", "20260828", "ACTIVE", 3, 1, 0),
                    new BpcsKanbanVO("001", "MO-100235", "20316", "王五制造", "20260821", "20260830", "ACTIVE", 0, 2, 1),
                    new BpcsKanbanVO("001", "MO-100236", "20317", "孙七贸易", "20260822", "20260901", "CLOSED", 0, 0, 4)
            );
        }
        String sql = statements.get("bpcs.order.kanban");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsKanbanVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsKanbanVO(
                    pickStr(row, "CONO"), pickStr(row, "ORNO"), pickStr(row, "CUST"),
                    pickStr(row, "CUNAME"), BpcsRowUtil.dateStr(row, "ORDTE"),
                    BpcsRowUtil.dateStr(row, "REQDTE"), pickStr(row, "STATUS"),
                    BpcsRowUtil.intOrNull(row, "PENDING_LINES"),
                    BpcsRowUtil.intOrNull(row, "PARTIAL_LINES"),
                    BpcsRowUtil.intOrNull(row, "SHIPPED_LINES")
            ));
        }
        return result;
    }
}
