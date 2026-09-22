package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsAlertRuleVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public PageResult<BpcsAlertRuleVO> getAlertRules(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsAlertRuleVO("DEF-2001", "螺柱 M12", "WH1", 50, 200, 0, "LOW_STOCK"),
                    new BpcsAlertRuleVO("DEF-2008", "润滑油", "WH1", 5000, 0, 3000, "OVER_STOCK")
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.inv.alertRules");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> {
                    int qtyOnHand = BpcsRowUtil.intVal(row, "QTYOH");
                    int safety = BpcsRowUtil.intVal(row, "SAFETY");
                    int maxStk = BpcsRowUtil.intVal(row, "MAXSTK");
                    String alertType = qtyOnHand < safety ? "LOW_STOCK" : "OVER_STOCK";
                    return new BpcsAlertRuleVO(
                            pickStr(row, "ITEM"),
                            pickStr(row, "ITDSC"),
                            pickStr(row, "WH"),
                            qtyOnHand,
                            safety,
                            maxStk,
                            alertType
                    );
                });
    }
}
