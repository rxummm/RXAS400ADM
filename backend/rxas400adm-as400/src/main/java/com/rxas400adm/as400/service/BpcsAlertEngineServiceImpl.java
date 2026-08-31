package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsAlertRuleVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㉜ 预警规则引擎实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsAlertEngineServiceImpl implements IBpcsAlertEngineService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsAlertRuleVO> getAlertRules(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockAlerts();
        }
        String sql = statements.get("bpcs.inv.alertRules");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsAlertRuleVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int qtyOnHand = BpcsRowUtil.intOrNull(row, "QTYOH");
            int safety = BpcsRowUtil.intOrNull(row, "SAFETY");
            int maxStk = BpcsRowUtil.intOrNull(row, "MAXSTK");
            String alertType = qtyOnHand < safety ? "LOW_STOCK" : "OVER_STOCK";
            result.add(new BpcsAlertRuleVO(
                    pickStr(row, "ITEM"),
                    pickStr(row, "ITDSC"),
                    pickStr(row, "WH"),
                    qtyOnHand,
                    safety,
                    maxStk,
                    alertType
            ));
        }
        return result;
    }

    private List<BpcsAlertRuleVO> mockAlerts() {
        return List.of(
                new BpcsAlertRuleVO("DEF-2001", "螺柱 M12", "WH1", 50, 200, 0, "LOW_STOCK"),
                new BpcsAlertRuleVO("DEF-2008", "润滑油", "WH1", 5000, 0, 3000, "OVER_STOCK")
        );
    }
}
