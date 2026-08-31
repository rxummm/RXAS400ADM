package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsStockValueVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㉚ 库存价值核算实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsStockValueServiceImpl implements IBpcsStockValueService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsStockValueVO> getValueReport(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockValue();
        }
        String sql = statements.get("bpcs.inv.valueReport");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsStockValueVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsStockValueVO(
                    pickStr(row, "ITEM"),
                    pickStr(row, "ITDSC"),
                    pickStr(row, "WH"),
                    BpcsRowUtil.intOrNull(row, "QTYOH"),
                    BpcsRowUtil.decOrNull(row, "UNITCOST"),
                    BpcsRowUtil.decOrNull(row, "STOCK_VALUE")
            ));
        }
        return result;
    }

    private List<BpcsStockValueVO> mockValue() {
        return List.of(
                new BpcsStockValueVO("DEF-2001", "螺柱 M12", "WH1", 5000, new java.math.BigDecimal("2.50"), new java.math.BigDecimal("12500.00")),
                new BpcsStockValueVO("DEF-2005", "密封圈", "WH2", 200, new java.math.BigDecimal("15.00"), new java.math.BigDecimal("3000.00"))
        );
    }
}
